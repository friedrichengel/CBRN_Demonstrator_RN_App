package de.thw.cbrn.strahler.measure

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.SystemClock
import de.thw.cbrn.strahler.model.Nuklid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Info about the currently dominant (highest-dose) source, for the side panel. */
data class BestSource(
    val name: String,
    val senderNr: Int,
    val nuklid: Nuklid,
    val rssiSmooth: Double,
    val maxDoseUsvh: Double,
)

/** Snapshot of the measurement state, published once per tick (~4 Hz). */
data class MeasurementState(
    val running: Boolean = false,
    val doseUsvh: Double = 0.0,
    val activeCount: Int = 0,
    val calibratingCount: Int = 0,
    val best: BestSource? = null,
)

/**
 * Turns the phone into a passive Sonde: continuously scans for Strahler advertising packets,
 * tracks each source's RSSI with the same two-stage smoothing (per-second median → moving
 * average) + peak model as the firmware, computes the dominant source's dose, and drives
 * Geiger click audio.
 *
 * Caller must hold scan permission before [start]. Source state is shared between the (main-
 * thread) scan callback and the tick coroutine, so all access is guarded by [lock].
 */
@SuppressLint("MissingPermission")
class MeasurementEngine(appContext: Context) {

    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter get() = bluetoothManager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    private val click = ClickPlayer()

    private val _state = MutableStateFlow(MeasurementState())
    val state: StateFlow<MeasurementState> = _state.asStateFlow()

    private val _audioEnabled = MutableStateFlow(true)
    val audioEnabled: StateFlow<Boolean> = _audioEnabled.asStateFlow()

    fun setAudioEnabled(enabled: Boolean) {
        _audioEnabled.value = enabled
    }

    @Volatile
    private var currentDose = 0.0

    private val lock = Any()
    private val sources = mutableMapOf<Int, Source>()

    private var scope: CoroutineScope? = null

    private class Source(val senderNr: Int) {
        var name: String = ""
        var nuklidId: Int = 1
        var maxDoseUsvh: Double = 0.0
        var txPower: Int = 0
        var valid: Boolean = false
        var lastSeen: Long = 0

        val binSamples = IntArray(DoseModel.RSSI_BIN_MAX_SAMPLES)
        var binCount: Int = 0
        var binStartMs: Long = 0
        val secBuf = DoubleArray(DoseModel.RSSI_AVG_SECS)
        var secBufIdx: Int = 0
        var secBufCount: Int = 0
        var rssiSmooth: Double = 0.0
        var rssiPeak: Double = 0.0
    }

    /** Median of the first [n] samples — mirrors the firmware's medianOf() in ble.cpp. */
    private fun medianOf(v: IntArray, n: Int): Double {
        val tmp = v.copyOf(n)
        tmp.sort()
        return if (n % 2 == 1) tmp[n / 2].toDouble()
        else (tmp[n / 2 - 1] + tmp[n / 2]) / 2.0
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) = handle(result)
        override fun onBatchScanResults(results: MutableList<ScanResult>) =
            results.forEach(::handle)
    }

    private fun handle(result: ScanResult) {
        val record = result.scanRecord ?: return
        // getManufacturerSpecificData strips the 2-byte company id → DoseModel offsets.
        val payload = record.getManufacturerSpecificData(DoseModel.COMPANY_ID) ?: return
        val packet = DoseModel.parse(payload) ?: return
        val name = record.deviceName ?: "Strahler-%03d".format(packet.senderNr)
        processPacket(packet, result.rssi, name, SystemClock.elapsedRealtime())
    }

    private fun processPacket(packet: DoseModel.Packet, rssi: Int, name: String, now: Long) {
        synchronized(lock) {
            val existing = sources[packet.senderNr]
            val s: Source = when {
                existing == null -> {
                    if (sources.size >= DoseModel.MAX_SOURCES) return
                    Source(packet.senderNr).also {
                        it.valid = true
                        it.rssiSmooth = rssi.toDouble()
                        it.rssiPeak = rssi.toDouble()
                        it.binStartMs = now
                        sources[packet.senderNr] = it
                    }
                }

                !existing.valid -> existing.also {
                    // Timeout recovery: reset smoothing, KEEP peak so the gauge doesn't
                    // jump to full scale on re-detection (delta_db = 0).
                    it.valid = true
                    it.binCount = 0
                    it.binStartMs = now
                    it.secBufIdx = 0
                    it.secBufCount = 0
                    it.rssiSmooth = rssi.toDouble()
                }

                else -> existing
            }

            s.name = name
            s.nuklidId = packet.nuklidId
            s.maxDoseUsvh = packet.maxDoseUsvh
            s.txPower = packet.txPower
            s.lastSeen = now

            // Stage 1: collect the sample into the current 1-second bin (buffer full →
            // drop the rest of the second, the median barely changes).
            if (s.binCount < DoseModel.RSSI_BIN_MAX_SAMPLES) {
                s.binSamples[s.binCount] = rssi
                s.binCount += 1
            }

            // Stage 2: bin complete → push bin MEDIAN into ring buffer (robust against
            // single outliers from reflection/shadowing), recompute moving average.
            if (now - s.binStartMs >= DoseModel.RSSI_BIN_MS && s.binCount > 0) {
                s.secBuf[s.secBufIdx] = medianOf(s.binSamples, s.binCount)
                s.secBufIdx = (s.secBufIdx + 1) % DoseModel.RSSI_AVG_SECS
                if (s.secBufCount < DoseModel.RSSI_AVG_SECS) s.secBufCount++
                var sum = 0.0
                for (i in 0 until s.secBufCount) sum += s.secBuf[i]
                s.rssiSmooth = sum / s.secBufCount
                if (s.rssiSmooth > s.rssiPeak) s.rssiPeak = s.rssiSmooth
                s.binCount = 0
                s.binStartMs = now
            }
        }
    }

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        var best: Source? = null
        var bestDose = -1.0
        var active = 0
        var calibrating = 0

        val info: BestSource?
        synchronized(lock) {
            for (s in sources.values) {
                if (s.valid && now - s.lastSeen > DoseModel.SOURCE_TIMEOUT_MS) s.valid = false
            }
            for (s in sources.values) {
                if (!s.valid) continue
                active++
                if (s.secBufCount < DoseModel.RSSI_AVG_SECS) {
                    calibrating++
                    continue
                }
                val d = DoseModel.calcDose(s.maxDoseUsvh, s.rssiSmooth, s.rssiPeak)
                if (d > bestDose) {
                    bestDose = d
                    best = s
                }
            }
            info = best?.let {
                BestSource(it.name, it.senderNr, Nuklid.fromId(it.nuklidId), it.rssiSmooth, it.maxDoseUsvh)
            }
        }

        val dose = if (best != null) bestDose else 0.0
        currentDose = dose
        _state.value = MeasurementState(
            running = true,
            doseUsvh = dose,
            activeCount = active,
            calibratingCount = calibrating,
            best = info,
        )
    }

    private suspend fun audioLoop() {
        while (coroutineScopeActive()) {
            if (_audioEnabled.value) {
                val cps = DoseModel.clickCps(currentDose)
                val meanMs = 1000.0 / cps
                click.click()
                // Poisson-light jitter: uniform over [mean/2, 1.5*mean].
                val jitter = (meanMs / 2.0 + Random.nextDouble() * meanMs).toLong().coerceAtLeast(1)
                delay(jitter)
            } else {
                delay(200)
            }
        }
    }

    private fun coroutineScopeActive(): Boolean = scope?.isActive == true

    fun start() {
        if (scope != null) return
        val sc = scanner
        if (sc == null || adapter?.isEnabled != true) {
            _state.value = MeasurementState(running = false)
            return
        }
        synchronized(lock) { sources.clear() }
        currentDose = 0.0

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setLegacy(false)
                    setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                }
            }
            .build()
        sc.startScan(null, settings, scanCallback)

        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = newScope
        _state.value = MeasurementState(running = true)
        newScope.launch {
            while (isActive) {
                tick()
                delay(TICK_MS)
            }
        }
        newScope.launch { audioLoop() }
    }

    fun stop() {
        runCatching { scanner?.stopScan(scanCallback) }
        scope?.cancel()
        scope = null
        _state.value = MeasurementState(running = false)
    }

    fun release() {
        stop()
        click.release()
    }

    companion object {
        private const val TICK_MS = 250L
    }
}
