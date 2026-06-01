package de.thw.cbrn.strahler.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import de.thw.cbrn.strahler.ble.BleManager
import de.thw.cbrn.strahler.ble.ConnectionState
import de.thw.cbrn.strahler.ble.DiscoveredDevice
import de.thw.cbrn.strahler.model.Einheit
import de.thw.cbrn.strahler.model.Nuklid
import de.thw.cbrn.strahler.model.StrahlerConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridges the [BleManager] to the Compose UI. All edits are applied optimistically to the
 * locally held config and immediately written through to the device over GATT.
 */
class StrahlerViewModel(app: Application) : AndroidViewModel(app) {

    private val ble = BleManager(app.applicationContext)

    val scanning: StateFlow<Boolean> = ble.scanning
    val devices: StateFlow<List<DiscoveredDevice>> = ble.devices
    val connection: StateFlow<ConnectionState> = ble.connection
    val config: StateFlow<StrahlerConfig?> = ble.config
    val status: StateFlow<String?> = ble.status

    fun isBluetoothEnabled() = ble.isBluetoothEnabled()
    fun consumeStatus() = ble.consumeStatus()

    fun startScan() = ble.startScan()
    fun stopScan() = ble.stopScan()
    fun connect(device: DiscoveredDevice) = ble.connect(device)
    fun disconnect() = ble.disconnect()

    // ---- Edits: update local config, then write the single changed value ----

    private inline fun edit(transform: (StrahlerConfig) -> StrahlerConfig): StrahlerConfig? {
        val current = ble.config.value ?: return null
        val updated = transform(current)
        ble.applyLocalConfig(updated)
        return updated
    }

    fun setSenderNr(value: Int) {
        val v = value.coerceIn(StrahlerConfig.SENDER_MIN, StrahlerConfig.SENDER_MAX)
        edit { it.copy(senderNr = v) } ?: return
        ble.writeSenderNr(v)
    }

    fun setNuklid(nuklid: Nuklid) {
        edit { it.copy(nuklid = nuklid) } ?: return
        ble.writeNuklid(nuklid)
    }

    fun setMaxDosis(value: Int) {
        val v = value.coerceIn(StrahlerConfig.DOSIS_MIN, StrahlerConfig.DOSIS_MAX)
        edit { it.copy(maxDosis = v) } ?: return
        ble.writeMaxDosis(v)
    }

    fun setEinheit(einheit: Einheit) {
        edit { it.copy(einheit = einheit) } ?: return
        ble.writeEinheit(einheit)
    }

    fun setAktiv(value: Boolean) {
        edit { it.copy(aktiv = value) } ?: return
        ble.writeAktiv(value)
    }

    fun setLongRange(value: Boolean) {
        edit { it.copy(longRange = value) } ?: return
        ble.writeLongRange(value)
    }

    override fun onCleared() {
        super.onCleared()
        ble.disconnect()
    }
}
