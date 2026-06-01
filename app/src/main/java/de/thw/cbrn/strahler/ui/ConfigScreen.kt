package de.thw.cbrn.strahler.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thw.cbrn.strahler.model.Einheit
import de.thw.cbrn.strahler.model.Nuklid
import de.thw.cbrn.strahler.model.StrahlerConfig
import de.thw.cbrn.strahler.ui.theme.EpaperBlack
import de.thw.cbrn.strahler.ui.theme.ThwBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: StrahlerViewModel,
    deviceName: String,
    modifier: Modifier = Modifier,
) {
    val config by viewModel.config.collectAsState()
    val cfg = config ?: return

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(deviceName, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = { viewModel.disconnect() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Trennen", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ThwBlue,
                titleContentColor = Color.White,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DevicePreview(cfg)

            SenderNrControl(cfg.senderNr, onChange = viewModel::setSenderNr)

            NuklidControl(cfg.nuklid, onChange = viewModel::setNuklid)

            DosisControl(cfg.maxDosis, onChange = viewModel::setMaxDosis)

            EinheitControl(cfg.einheit, onChange = viewModel::setEinheit)

            ToggleControl(
                title = "Aktiv",
                description = if (cfg.aktiv) "Sendet (Advertising an)" else "Aus",
                checked = cfg.aktiv,
                onChange = viewModel::setAktiv,
            )

            ToggleControl(
                title = "Reichweite",
                description = if (cfg.longRange) "Lang (Coded PHY, ~150–200 m)"
                else "Normal (1 Mbps, ~30–50 m)",
                checked = cfg.longRange,
                onChange = viewModel::setLongRange,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

/** A faithful, mobile-scaled rendering of the device's e-paper status screen. */
@Composable
private fun DevicePreview(cfg: StrahlerConfig) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, EpaperBlack, RoundedCornerShape(4.dp)),
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(4.dp))
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cfg.senderLabel,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = EpaperBlack,
                )
                StatusFlags(cfg)
            }
            HorizontalDivider(color = EpaperBlack, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))
            Text(
                cfg.nuklid.displayName,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = EpaperBlack,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Max. Dosisleistung:",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = EpaperBlack,
            )
            Text(
                cfg.doseLabel,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = EpaperBlack,
            )
        }
    }
}

@Composable
private fun StatusFlags(cfg: StrahlerConfig) {
    val text = buildString {
        append(if (cfg.aktiv) "AN" else "AUS")
        if (cfg.longRange) append("/LR")
    }
    Box(
        modifier = Modifier
            .border(1.5.dp, EpaperBlack, RoundedCornerShape(3.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = EpaperBlack,
        )
    }
}

// ---- Individual controls -------------------------------------------------

@Composable
private fun ControlCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = ThwBlue,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SenderNrControl(value: Int, onChange: (Int) -> Unit) {
    ControlCard("Sender-Nr") {
        NumberStepper(
            value = value,
            min = StrahlerConfig.SENDER_MIN,
            max = StrahlerConfig.SENDER_MAX,
            steps = listOf(1, 10),
            onChange = onChange,
        )
    }
}

@Composable
private fun DosisControl(value: Int, onChange: (Int) -> Unit) {
    ControlCard("Max. Dosisleistung") {
        NumberStepper(
            value = value,
            min = StrahlerConfig.DOSIS_MIN,
            max = StrahlerConfig.DOSIS_MAX,
            steps = listOf(1, 10, 100, 1000),
            onChange = onChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuklidControl(value: Nuklid, onChange: (Nuklid) -> Unit) {
    ControlCard("Nuklid") {
        EnumDropdown(
            selectedLabel = value.displayName,
            options = Nuklid.entries.map { it.displayName to it },
            onSelected = onChange,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EinheitControl(value: Einheit, onChange: (Einheit) -> Unit) {
    ControlCard("Einheit") {
        EnumDropdown(
            selectedLabel = value.label,
            options = Einheit.entries.map { it.label to it },
            onSelected = onChange,
        )
    }
}

@Composable
private fun ToggleControl(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = ThwBlue)
                Text(
                    description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

// ---- Reusable building blocks --------------------------------------------

/**
 * −/+ stepper plus a direct numeric entry field. [steps] gives one −/+ button pair per
 * magnitude (e.g. 1/10/100/1000), echoing the device's edit-mode acceleration.
 */
@Composable
private fun NumberStepper(
    value: Int,
    min: Int,
    max: Int,
    steps: List<Int>,
    onChange: (Int) -> Unit,
) {
    // Local text mirror so the field can be edited freely before committing.
    var text by remember(value) { mutableStateOf(value.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                text = new.filter { it.isDigit() }.take(5)
                text.toIntOrNull()?.let { onChange(it.coerceIn(min, max)) }
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number,
            ),
            suffix = { Text("($min–$max)") },
            modifier = Modifier.fillMaxWidth(),
        )
        steps.forEach { step ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = { onChange((value - step).coerceIn(min, max)) },
                    enabled = value > min,
                ) { Icon(Icons.Filled.Remove, contentDescription = "minus $step") }
                Text(
                    "± $step",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                )
                FilledTonalIconButton(
                    onClick = { onChange((value + step).coerceIn(min, max)) },
                    enabled = value < max,
                ) { Icon(Icons.Filled.Add, contentDescription = "plus $step") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    selectedLabel: String,
    options: List<Pair<String, T>>,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, item) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(item)
                        expanded = false
                    },
                )
            }
        }
    }
}
