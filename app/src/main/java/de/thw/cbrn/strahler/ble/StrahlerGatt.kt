package de.thw.cbrn.strahler.ble

import java.util.UUID

/**
 * BLE/GATT constants for the Strahler device, taken directly from the firmware
 * (CBRN_Demonstrator_RN_Probe, ble_radio.cpp).
 *
 * Service 0x1900 exposes six open (no-pairing) read/write characteristics.
 * Note the endianness quirk: Max-Dosis over GATT is **little-endian uint16**
 * (the advertising manufacturer-data uses big-endian — that path is not used here).
 */
object StrahlerGatt {

    /** BLE devices advertise as "Strahler-NNN" (NNN = sender number). */
    const val NAME_PREFIX = "Strahler"

    /** Expands a 16-bit Bluetooth SIG short UUID to its full 128-bit form. */
    private fun shortUuid(short16: Int): UUID =
        UUID.fromString("0000%04x-0000-1000-8000-00805f9b34fb".format(short16))

    val SERVICE: UUID = shortUuid(0x1900)

    val CHAR_SENDER_NR: UUID = shortUuid(0x2B00) // uint8  1..254
    val CHAR_NUKLID: UUID = shortUuid(0x2B01)    // uint8  1..8
    val CHAR_MAX_DOSIS: UUID = shortUuid(0x2B02) // uint16 LE 0..65535
    val CHAR_AKTIV: UUID = shortUuid(0x2B03)     // bool   0/1
    val CHAR_LONG_RANGE: UUID = shortUuid(0x2B04) // bool  0/1
    val CHAR_EINHEIT: UUID = shortUuid(0x2B05)   // uint8  0..3

    /** All characteristics we read on connect, in display order. */
    val ALL_CHARS: List<UUID> = listOf(
        CHAR_SENDER_NR,
        CHAR_NUKLID,
        CHAR_MAX_DOSIS,
        CHAR_AKTIV,
        CHAR_LONG_RANGE,
        CHAR_EINHEIT,
    )
}
