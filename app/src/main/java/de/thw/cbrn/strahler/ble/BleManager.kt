package de.thw.cbrn.strahler.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.thw.cbrn.strahler.model.Einheit
import de.thw.cbrn.strahler.model.Nuklid
import de.thw.cbrn.strahler.model.StrahlerConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.UUID

/** A Strahler found during scanning. */
data class DiscoveredDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val device: BluetoothDevice,
)

/** High-level connection lifecycle exposed to the UI. */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    /** Connected and currently reading the initial configuration. */
    data object Reading : ConnectionState
    /** Connected and config loaded — ready to edit. */
    data class Ready(val deviceName: String) : ConnectionState
    data class Failed(val reason: String) : ConnectionState
}

/**
 * Owns all Bluetooth interaction with a Strahler device.
 *
 * Android allows only a single outstanding GATT operation at a time, so reads and writes
 * are serialised through [opQueue]; the next op is dispatched only from the previous op's
 * completion callback. Callers must hold the relevant runtime permissions (BLUETOOTH_SCAN /
 * BLUETOOTH_CONNECT on API 31+, or location on older) before invoking scan/connect.
 */
@SuppressLint("MissingPermission")
class BleManager(private val appContext: Context) {

    private val tag = "StrahlerBle"
    private val main = Handler(Looper.getMainLooper())

    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter get() = bluetoothManager.adapter
    private val scanner get() = adapter?.bluetoothLeScanner

    // ---- Observable state -------------------------------------------------

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _config = MutableStateFlow<StrahlerConfig?>(null)
    val config: StateFlow<StrahlerConfig?> = _config.asStateFlow()

    /** One-shot, human-readable status/toast text (cleared by the UI after showing). */
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun consumeStatus() {
        _status.value = null
    }

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    // ---- Scanning ---------------------------------------------------------

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach(::handleScanResult)
        }

        override fun onScanFailed(errorCode: Int) {
            _scanning.value = false
            _status.value = "Scan fehlgeschlagen (Code $errorCode)"
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val name = result.scanRecord?.deviceName ?: runCatching { result.device.name }.getOrNull()
        if (name == null || !name.startsWith(StrahlerGatt.NAME_PREFIX, ignoreCase = true)) return

        val found = DiscoveredDevice(
            address = result.device.address,
            name = name,
            rssi = result.rssi,
            device = result.device,
        )
        val current = _devices.value
        val idx = current.indexOfFirst { it.address == found.address }
        _devices.value =
            if (idx >= 0) current.toMutableList().also { it[idx] = found }
            else current + found
    }

    fun startScan() {
        if (scanner == null || adapter?.isEnabled != true) {
            _status.value = "Bluetooth ist ausgeschaltet"
            return
        }
        if (_scanning.value) return
        _devices.value = emptyList()

        // setLegacy(false) reports BOTH legacy and extended (Coded-PHY / Long-Range)
        // advertisers, so the app finds Strahler in either Reichweite mode.
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setLegacy(false)
                    setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                }
            }
            .build()

        _scanning.value = true
        // No ScanFilter: the Strahler does not advertise its 0x1900 service UUID, so we
        // filter by advertised name in handleScanResult() instead.
        scanner?.startScan(null, settings, scanCallback)

        // Auto-stop after 15 s to save battery; UI can restart.
        main.postDelayed({ stopScan() }, SCAN_DURATION_MS)
    }

    fun stopScan() {
        if (!_scanning.value) return
        _scanning.value = false
        runCatching { scanner?.stopScan(scanCallback) }
    }

    // ---- Connection -------------------------------------------------------

    private var gatt: BluetoothGatt? = null

    /** Accumulates characteristic values read during the initial sync. */
    private val readValues = mutableMapOf<UUID, ByteArray>()

    private val opQueue = ArrayDeque<GattOp>()
    private var opInFlight = false

    private sealed interface GattOp {
        data class Read(val uuid: UUID) : GattOp
        data class Write(val uuid: UUID, val bytes: ByteArray) : GattOp
    }

    fun connect(device: DiscoveredDevice) {
        stopScan()
        disconnect()
        _connection.value = ConnectionState.Connecting
        readValues.clear()
        gatt = device.device.connectGatt(
            appContext,
            /* autoConnect = */ false,
            gattCallback,
            BluetoothDevice.TRANSPORT_LE,
        )
    }

    fun disconnect() {
        opQueue.clear()
        opInFlight = false
        gatt?.let {
            runCatching { it.disconnect() }
            runCatching { it.close() }
        }
        gatt = null
        _config.value = null
        if (_connection.value !is ConnectionState.Failed) {
            _connection.value = ConnectionState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connection.value = ConnectionState.Reading
                // Small delay improves service-discovery reliability on some phones.
                main.postDelayed({ runCatching { g.discoverServices() } }, 200)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                main.post {
                    val wasReady = _connection.value is ConnectionState.Ready ||
                        _connection.value is ConnectionState.Reading
                    runCatching { g.close() }
                    if (gatt === g) gatt = null
                    _config.value = null
                    opQueue.clear()
                    opInFlight = false
                    if (_connection.value !is ConnectionState.Failed) {
                        _connection.value = ConnectionState.Disconnected
                    }
                    if (wasReady) _status.value = "Verbindung getrennt"
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            main.post {
                val service = g.getService(StrahlerGatt.SERVICE)
                if (status != BluetoothGatt.GATT_SUCCESS || service == null) {
                    fail("Strahler-Dienst (0x1900) nicht gefunden")
                    return@post
                }
                // Queue an initial read of every characteristic to load the current config.
                StrahlerGatt.ALL_CHARS.forEach { opQueue.add(GattOp.Read(it)) }
                processNextOp()
            }
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            // API 33+ path: value delivered as a parameter.
            finishRead(characteristic.uuid, value, status)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            // Pre-API-33 path: value lives on the characteristic. Copy before posting.
            finishRead(characteristic.uuid, characteristic.value ?: ByteArray(0), status)
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            main.post {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    _status.value = "Schreiben fehlgeschlagen (Status $status)"
                }
                opInFlight = false
                processNextOp()
            }
        }
    }

    private fun finishRead(uuid: UUID, value: ByteArray, status: Int) {
        val copy = value.copyOf()
        main.post {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readValues[uuid] = copy
            } else {
                Log.w(tag, "Read of $uuid failed: status=$status")
            }
            opInFlight = false
            processNextOp()
        }
    }

    /** Must run on the main thread — all queue/state access is serialised there. */
    private fun processNextOp() {
        if (opInFlight) return
        val g = gatt ?: return
        val op = opQueue.poll()
        if (op == null) {
            // Queue drained. If we were doing the initial sync, publish the config now.
            if (_connection.value is ConnectionState.Reading) publishConfigAndReady(g)
            return
        }
        opInFlight = true
        when (op) {
            is GattOp.Read -> {
                val chr = characteristic(g, op.uuid)
                if (chr == null || !g.readCharacteristic(chr)) {
                    opInFlight = false
                    processNextOp()
                }
            }

            is GattOp.Write -> {
                val chr = characteristic(g, op.uuid)
                if (chr == null) {
                    opInFlight = false
                    processNextOp()
                    return
                }
                val ok = writeCharacteristic(g, chr, op.bytes)
                if (!ok) {
                    opInFlight = false
                    _status.value = "Schreiben konnte nicht gestartet werden"
                    processNextOp()
                }
            }
        }
    }

    private fun characteristic(g: BluetoothGatt, uuid: UUID): BluetoothGattCharacteristic? =
        g.getService(StrahlerGatt.SERVICE)?.getCharacteristic(uuid)

    @Suppress("DEPRECATION")
    private fun writeCharacteristic(
        g: BluetoothGatt,
        chr: BluetoothGattCharacteristic,
        bytes: ByteArray,
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(
                chr,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            chr.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            chr.value = bytes
            g.writeCharacteristic(chr)
        }
    }

    private fun publishConfigAndReady(g: BluetoothGatt) {
        val cfg = StrahlerConfig(
            senderNr = readUInt8(StrahlerGatt.CHAR_SENDER_NR, default = 1),
            nuklid = Nuklid.fromId(readUInt8(StrahlerGatt.CHAR_NUKLID, default = 1)),
            maxDosis = readUInt16Le(StrahlerGatt.CHAR_MAX_DOSIS, default = 10),
            einheit = Einheit.fromIndex(readUInt8(StrahlerGatt.CHAR_EINHEIT, default = 1)),
            aktiv = readBool(StrahlerGatt.CHAR_AKTIV, default = true),
            longRange = readBool(StrahlerGatt.CHAR_LONG_RANGE, default = false),
        )
        _config.value = cfg
        val name = runCatching { g.device.name }.getOrNull() ?: "Strahler"
        _connection.value = ConnectionState.Ready(name)
    }

    private fun fail(reason: String) {
        _connection.value = ConnectionState.Failed(reason)
        _status.value = reason
        disconnect()
    }

    // ---- Byte (de)coding --------------------------------------------------

    private fun readUInt8(uuid: UUID, default: Int): Int {
        val b = readValues[uuid] ?: return default
        return if (b.isNotEmpty()) b[0].toInt() and 0xFF else default
    }

    private fun readUInt16Le(uuid: UUID, default: Int): Int {
        val b = readValues[uuid] ?: return default
        return when {
            b.size >= 2 -> (b[0].toInt() and 0xFF) or ((b[1].toInt() and 0xFF) shl 8)
            b.size == 1 -> b[0].toInt() and 0xFF
            else -> default
        }
    }

    private fun readBool(uuid: UUID, default: Boolean): Boolean {
        val b = readValues[uuid] ?: return default
        return if (b.isNotEmpty()) b[0].toInt() != 0 else default
    }

    // ---- Public write API (used by the ViewModel) -------------------------

    fun writeSenderNr(value: Int) =
        enqueueWrite(StrahlerGatt.CHAR_SENDER_NR, byteArrayOf(value.coerceIn(1, 254).toByte()))

    fun writeNuklid(nuklid: Nuklid) =
        enqueueWrite(StrahlerGatt.CHAR_NUKLID, byteArrayOf(nuklid.id.toByte()))

    fun writeMaxDosis(value: Int) {
        val v = value.coerceIn(0, 0xFFFF)
        enqueueWrite(
            StrahlerGatt.CHAR_MAX_DOSIS,
            byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte()), // little-endian
        )
    }

    fun writeEinheit(einheit: Einheit) =
        enqueueWrite(StrahlerGatt.CHAR_EINHEIT, byteArrayOf(einheit.index.toByte()))

    fun writeAktiv(value: Boolean) =
        enqueueWrite(StrahlerGatt.CHAR_AKTIV, byteArrayOf(if (value) 1 else 0))

    fun writeLongRange(value: Boolean) =
        enqueueWrite(StrahlerGatt.CHAR_LONG_RANGE, byteArrayOf(if (value) 1 else 0))

    /** Optimistically reflect an edited config in the UI before the write is confirmed. */
    fun applyLocalConfig(cfg: StrahlerConfig) {
        if (_connection.value is ConnectionState.Ready) _config.value = cfg
    }

    private fun enqueueWrite(uuid: UUID, bytes: ByteArray) {
        if (gatt == null || _connection.value !is ConnectionState.Ready) {
            _status.value = "Nicht verbunden"
            return
        }
        opQueue.add(GattOp.Write(uuid, bytes))
        main.post { processNextOp() }
    }

    companion object {
        private const val SCAN_DURATION_MS = 15_000L
    }
}
