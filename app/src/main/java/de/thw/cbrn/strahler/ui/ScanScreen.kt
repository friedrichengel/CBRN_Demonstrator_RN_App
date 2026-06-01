package de.thw.cbrn.strahler.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.thw.cbrn.strahler.ble.ConnectionState
import de.thw.cbrn.strahler.ble.DiscoveredDevice
import de.thw.cbrn.strahler.ui.theme.ThwBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: StrahlerViewModel,
    connection: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val scanning by viewModel.scanning.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val connecting = connection is ConnectionState.Connecting ||
        connection is ConnectionState.Reading

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("CBRN-RN Strahler", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Konfigurator",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ThwBlue,
                titleContentColor = Color.White,
            ),
        )

        Box(modifier = Modifier.weight(1f)) {
            if (devices.isEmpty()) {
                EmptyState(scanning)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(devices, key = { it.address }) { device ->
                        DeviceRow(
                            device = device,
                            enabled = !connecting,
                            onClick = { viewModel.connect(device) },
                        )
                    }
                }
            }

            if (connecting) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (connection is ConnectionState.Reading) "Lese Konfiguration…"
                        else "Verbinde…",
                    )
                }
            }
        }

        ScanButton(
            scanning = scanning,
            enabled = !connecting,
            onStart = { viewModel.startScan() },
            onStop = { viewModel.stopScan() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

@Composable
private fun ScanButton(
    scanning: Boolean,
    enabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = { if (scanning) onStop() else onStart() },
        enabled = enabled,
        modifier = modifier,
    ) {
        if (scanning) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
            )
            Spacer(Modifier.size(10.dp))
            Text("Suche läuft… (Stoppen)")
        } else {
            Icon(Icons.Filled.Bluetooth, contentDescription = null)
            Spacer(Modifier.size(10.dp))
            Text("Strahler suchen")
        }
    }
}

@Composable
private fun DeviceRow(
    device: DiscoveredDevice,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    device.address,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SignalIndicator(device.rssi)
            Spacer(Modifier.size(8.dp))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Verbinden",
                tint = ThwBlue,
            )
        }
    }
}

@Composable
private fun SignalIndicator(rssi: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.SignalCellularAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(4.dp))
        Text("$rssi dBm", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun EmptyState(scanning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Bluetooth,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (scanning) "Suche nach Strahler-Geräten…"
            else "Kein Gerät gefunden.\nTippe auf „Strahler suchen“.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
