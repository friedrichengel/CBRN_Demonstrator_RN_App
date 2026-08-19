package de.thw.cbrn.strahler.model

/**
 * Domain model for a Strahler (radiation-source emulator) device.
 *
 * Mirrors the firmware config struct from CBRN_Demonstrator_RN_Probe (config.h) and the
 * GATT characteristics it exposes. Keep the ordering/IDs in sync with the firmware:
 *  - Nuklid IDs are 1-based (matches mfg-data byte 4 and characteristic 0x2B01).
 *  - Einheit indices are 0-based (matches mfg-data byte 7 and characteristic 0x2B05).
 */

/** Nuclide table — index in [id] matches the firmware Nuklid enum (1..9). */
enum class Nuklid(val id: Int, val displayName: String) {
    CS_137(1, "Cs-137"),
    CO_60(2, "Co-60"),
    AM_241(3, "Am-241"),
    IR_192(4, "Ir-192"),
    RA_226(5, "Ra-226"),
    TH_232(6, "Th-232"),
    U_238(7, "U-238"),
    PU_239(8, "Pu-239"),
    SE_75(9, "Se-75");

    companion object {
        fun fromId(id: Int): Nuklid = entries.firstOrNull { it.id == id } ?: CS_137
    }
}

/** Dose-rate unit table — [index] matches the firmware unit_table order (0..3). */
enum class Einheit(val index: Int, val label: String) {
    NSV_H(0, "nSv/h"),
    USV_H(1, "µSv/h"),
    MSV_H(2, "mSv/h"),
    SV_H(3, "Sv/h");

    companion object {
        fun fromIndex(index: Int): Einheit = entries.firstOrNull { it.index == index } ?: USV_H
    }
}

/**
 * Full Strahler configuration as read from / written to the device.
 *
 * Field ranges are enforced against the firmware's `constrain()` limits:
 *  - [senderNr]  1..254
 *  - [maxDosis]  0..65535 (uint16)
 */
data class StrahlerConfig(
    val senderNr: Int = 1,
    val nuklid: Nuklid = Nuklid.CS_137,
    val maxDosis: Int = 10,
    val einheit: Einheit = Einheit.USV_H,
    val aktiv: Boolean = true,
    val longRange: Boolean = false,
) {
    /** Sender label as shown on the device display, e.g. "Q001". */
    val senderLabel: String get() = "Q%03d".format(senderNr)

    /** Human-readable dose, e.g. "100 µSv/h". */
    val doseLabel: String get() = "$maxDosis ${einheit.label}"

    companion object {
        const val SENDER_MIN = 1
        const val SENDER_MAX = 254
        const val DOSIS_MIN = 0
        const val DOSIS_MAX = 65535
    }
}
