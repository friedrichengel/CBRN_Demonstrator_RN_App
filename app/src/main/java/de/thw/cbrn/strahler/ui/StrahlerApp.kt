package de.thw.cbrn.strahler.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import de.thw.cbrn.strahler.ble.ConnectionState

/** Runtime permissions required to scan & connect, depending on the OS version. */
private val blePermissions: List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        listOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }

private enum class Tab { Measure, Configure }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StrahlerApp(viewModel: StrahlerViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val status by viewModel.status.collectAsState()
    var tab by remember { mutableStateOf(Tab.Measure) }

    LaunchedEffect(status) {
        status?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatus()
        }
    }

    val permissions = rememberMultiplePermissionsState(blePermissions)
    val granted = permissions.allPermissionsGranted

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (granted) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == Tab.Measure,
                        onClick = { tab = Tab.Measure },
                        icon = { Icon(Icons.Filled.Sensors, contentDescription = null) },
                        label = { Text("Messen") },
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Configure,
                        onClick = { tab = Tab.Configure },
                        icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
                        label = { Text("Einrichten") },
                    )
                }
            }
        },
    ) { padding ->
        if (!granted) {
            PermissionGate(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onRequest = { permissions.launchMultiplePermissionRequest() },
            )
            return@Scaffold
        }

        when (tab) {
            Tab.Measure -> GeigerScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding),
            )

            Tab.Configure -> {
                val connection by viewModel.connection.collectAsState()
                if (connection is ConnectionState.Ready) {
                    ConfigScreen(
                        viewModel = viewModel,
                        deviceName = (connection as ConnectionState.Ready).deviceName,
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    ScanScreen(
                        viewModel = viewModel,
                        connection = connection,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGate(modifier: Modifier = Modifier, onRequest: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Bluetooth-Berechtigung erforderlich",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Die App benötigt die Bluetooth-Berechtigung, um Strahler-Geräte " +
                "zu finden, zu messen und über GATT zu konfigurieren.",
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequest) { Text("Berechtigung erteilen") }
    }
}
