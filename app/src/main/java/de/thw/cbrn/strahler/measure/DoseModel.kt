package de.thw.cbrn.strahler.measure

import kotlin.math.pow

/**
 * Faithful port of the CBRN_Demonstrator_RN_Sonde dose/parsing math, so the phone behaves
 * like the hardware Sonde when it receives the same "Strahler-NNN" advertising packets.
 *
 * IMPORTANT — byte offsets: the firmware reads the *full* manufacturer data including the
 * 2-byte company id (md[0..1] = 0xFF 0xFF). Android's
 * [android.bluetooth.le.ScanRecord.getManufacturerSpecificData] strips the company id and
 * returns the payload starting at the firmware's md[2]. All offsets here are therefore
 * shifted down by 2 versus the firmware:
 *
 *   payload[0] = protocol version (must be 0x02)   (firmware md[2])
 *   payload[1] = sender number                     (firmware md[3])
 *   payload[2] = nuklid id (1..9)                  (firmware md[4])
 *   payload[3..4] = max dose, BIG-endian uint16    (firmware md[5..6])
 *   payload[5] = unit index (0..3)                 (firmware md[7])
 *   payload[6] = tx power, signed int8 (dBm)       (firmware md[8])
 *   payload[7] = XOR-CRC over payload[0..6]        (firmware md[9])
 */
object DoseModel {

    const val COMPANY_ID = 0xFFFF
    const val PROTOCOL_VERSION = 0x02

    // --- Firmware constants (config.h) ---
    const val DOSE_PATH_LOSS_N = 12.0
    const val RSSI_BIN_MS = 1000L
    const val RSSI_AVG_SECS = 3
    const val RSSI_BIN_MAX_SAMPLES = 16
    const val SOURCE_TIMEOUT_MS = 5000L
    const val MAX_SOURCES = 16

    /** Decoded advertising packet from a Strahler source. */
    data class Packet(
        val senderNr: Int,
        val nuklidId: Int,
        val maxDoseUsvh: Double,
        val unitIndex: Int,
        val txPower: Int,
    )

    /**
     * Parses the company-id-stripped manufacturer payload. Returns null if the packet is not
     * a valid v2 Strahler packet (wrong version, bad CRC, or unknown unit).
     */
    fun parse(payload: ByteArray?): Packet? {
        if (payload == null || payload.size < 8) return null
        fun u(i: Int) = payload[i].toInt() and 0xFF
        if (u(0) != PROTOCOL_VERSION) return null

        val crc = u(0) xor u(1) xor u(2) xor u(3) xor u(4) xor u(5) xor u(6)
        if (crc != u(7)) return null

        val maxRaw = (u(3) shl 8) or u(4) // big-endian
        val unitIndex = u(5)
        val maxDoseUsvh = when (unitIndex) {
            0 -> maxRaw * 0.001    // nSv/h
            1 -> maxRaw.toDouble() // µSv/h
            2 -> maxRaw * 1000.0   // mSv/h
            3 -> maxRaw * 1.0e6    // Sv/h
            else -> return null
        }
        return Packet(
            senderNr = u(1),
            nuklidId = u(2),
            maxDoseUsvh = maxDoseUsvh,
            unitIndex = unitIndex,
            txPower = payload[6].toInt(), // signed
        )
    }

    /**
     * Current dose in µSv/h from the path-loss model:
     *   I = I_max * 10^(delta_db / (5*n)), delta_db = rssiSmooth - rssiPeak (clamped <= 0).
     */
    fun calcDose(maxDoseUsvh: Double, rssiSmooth: Double, rssiPeak: Double): Double {
        val delta = (rssiSmooth - rssiPeak).coerceAtMost(0.0)
        return maxDoseUsvh * 10.0.pow(delta / (5.0 * DOSE_PATH_LOSS_N))
    }

    /** Geiger clicks per second: 0.3 cps background + dose-proportional, capped at 50 cps. */
    fun clickCps(doseUsvh: Double): Double = (0.3 + doseUsvh * 0.5).coerceAtMost(50.0)

    /** Result of auto-ranging the gauge scale to a readable unit. */
    data class GaugeRange(
        val displayVal: Double,
        val gaugeMax: Double,
        val unit: String,
        val fraction: Double,
    )

    /**
     * Picks the gauge unit/scale from the source's advertised full-scale dose, mirroring the
     * firmware autoRange(). [fraction] (0..1) drives needle position and color.
     */
    fun autoRange(doseUsvh: Double, maxDoseUsvhIn: Double): GaugeRange {
        val maxDoseUsvh = if (maxDoseUsvhIn <= 0.0) 1.0 else maxDoseUsvhIn
        val fraction = (doseUsvh / maxDoseUsvh).coerceIn(0.0, 1.0)
        return when {
            maxDoseUsvh < 1.0 ->
                GaugeRange(doseUsvh * 1000.0, maxDoseUsvh * 1000.0, "nSv/h", fraction)
            maxDoseUsvh < 1000.0 ->
                GaugeRange(doseUsvh, maxDoseUsvh, "µSv/h", fraction)
            maxDoseUsvh < 1.0e6 ->
                GaugeRange(doseUsvh / 1000.0, maxDoseUsvh / 1000.0, "mSv/h", fraction)
            else ->
                GaugeRange(doseUsvh / 1.0e6, maxDoseUsvh / 1.0e6, "Sv/h", fraction)
        }
    }
}
