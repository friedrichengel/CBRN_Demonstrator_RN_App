package de.thw.cbrn.strahler.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun StrahlerApp(viewModel: StrahlerViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    val status by viewModel.status.collectAsState()

    LaunchedEffect(status) {
        status?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val permissions = rememberMultiplePermissionsState(blePermissions)

        if (!permissions.allPermissionsGranted) {
            PermissionGate(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onRequest = { permissions.launchMultiplePermissionRequest() },
            )
            return@Scaffold
        }

        val connection by viewModel.connection.collectAsState()
        when (connection) {
            is ConnectionState.Ready -> ConfigScreen(
                viewModel = viewModel,
                deviceName = (connection as ConnectionState.Ready).deviceName,
                modifier = Modifier.padding(padding),
            )

            else -> ScanScreen(
                viewModel = viewModel,
                connection = connection,
                modifier = Modifier.padding(padding),
            )
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
                "zu finden und über GATT zu konfigurieren.",
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequest) { Text("Berechtigung erteilen") }
    }
}
