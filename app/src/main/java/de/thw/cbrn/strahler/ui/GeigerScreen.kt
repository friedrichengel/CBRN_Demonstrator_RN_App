package de.thw.cbrn.strahler.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.thw.cbrn.strahler.measure.MeasurementState
import de.thw.cbrn.strahler.measure.DoseModel
import de.thw.cbrn.strahler.ui.theme.ThwBlue
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Gauge color zones (fraction of full scale), matching the firmware thresholds.
private val ZoneGreen = Color(0xFF2EA84F)
private val ZoneYellow = Color(0xFFFFC107)
private val ZoneOrange = Color(0xFFFF6D00)
private val ZoneRed = Color(0xFFD32F2F)

private fun zoneColor(fraction: Double): Color = when {
    fraction < 0.50 -> ZoneGreen
    fraction < 0.75 -> ZoneYellow
    fraction < 0.90 -> ZoneOrange
    else -> ZoneRed
}

/** fraction (0..1) → needle/scale angle in degrees: 195° (empty) … −15° (full). */
private fun gaugeAngle(fraction: Double): Double = 195.0 - fraction.coerceIn(0.0, 1.0) * 210.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeigerScreen(viewModel: StrahlerViewModel, modifier: Modifier = Modifier) {
    // Run the passive scan only while this screen is on-screen.
    DisposableEffect(Unit) {
        viewModel.startMeasuring()
        onDispose { viewModel.stopMeasuring() }
    }

    val state by viewModel.measurementState.collectAsState()
    val audioEnabled by viewModel.audioEnabled.collectAsState()

    val range = DoseModel.autoRange(state.doseUsvh, state.best?.maxDoseUsvh ?: 1.0)
    val calibratingOnly = state.best == null && state.calibratingCount > 0

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Messen", style = MaterialTheme.typography.titleLarge) },
            actions = {
                IconButton(onClick = { viewModel.setAudioEnabled(!audioEnabled) }) {
                    Icon(
                        if (audioEnabled) Icons.Filled.VolumeUp
                        else Icons.Filled.VolumeOff,
                        contentDescription = if (audioEnabled) "Ton aus" else "Ton an",
                        tint = Color.White,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ThwBlue,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Gauge(
                fraction = range.fraction,
                gaugeMax = range.gaugeMax,
                unit = range.unit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.55f),
            )

            Spacer(Modifier.height(8.dp))

            // Big numeric readout.
            val color = zoneColor(range.fraction)
            Text(
                text = when {
                    calibratingOnly -> "----"
                    else -> formatDose(range.displayVal)
                },
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
                color = if (calibratingOnly) ZoneYellow else color,
            )
            Text(
                range.unit,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            InfoPanel(state)
        }
    }
}

@Composable
private fun InfoPanel(state: MeasurementState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow("Quellen", state.activeCount.toString())
            val best = state.best
            when {
                best != null -> {
                    InfoRow("Nächste", best.name)
                    InfoRow("Nuklid", best.nuklid.displayName)
                    InfoRow("RSSI", "%.0f dBm".format(best.rssiSmooth))
                }

                state.calibratingCount > 0 -> {
                    Spacer(Modifier.height(6.dp))
                    Text("Kalibrierung…", color = ZoneYellow, fontWeight = FontWeight.Bold)
                }

                else -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (state.running) "Kein Signal" else "Messung inaktiv (Bluetooth aus?)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Gauge(
    fraction: Double,
    gaugeMax: Double,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val needleColor = zoneColor(fraction)
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGauge(fraction, gaugeMax, needleColor)
        }
    }
}

private fun DrawScope.drawGauge(fraction: Double, gaugeMax: Double, needleColor: Color) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.82f
    val r = min(w * 0.42f, h * 0.62f)

    fun point(angleDeg: Double, radius: Float): Offset {
        val rad = Math.toRadians(angleDeg)
        return Offset(cx + (radius * cos(rad)).toFloat(), cy - (radius * sin(rad)).toFloat())
    }

    // Colored arc band, drawn as short segments like the firmware.
    val arcWidth = r * 0.12f
    var a = 195.0
    while (a > -15.0) {
        val a1 = (a - 1.5).coerceAtLeast(-15.0)
        val frac = (195.0 - (a + a1) / 2.0) / 210.0
        drawLine(
            color = zoneColor(frac),
            start = point(a, r),
            end = point(a1, r),
            strokeWidth = arcWidth,
            cap = StrokeCap.Butt,
        )
        a = a1
    }

    // Major + minor ticks and numeric labels.
    val labelPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.GRAY
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = r * 0.10f
    }
    for (i in 0..10) {
        val f = i / 10.0
        val ang = gaugeAngle(f)
        drawLine(
            color = Color.DarkGray,
            start = point(ang, r - arcWidth / 2),
            end = point(ang, r - arcWidth * 1.6f),
            strokeWidth = r * 0.012f,
        )
        // minor ticks
        if (i < 10) {
            for (j in 1..3) {
                val sang = gaugeAngle((i + j * 0.25) / 10.0)
                drawLine(
                    color = Color(0xFFBBBBBB),
                    start = point(sang, r - arcWidth / 2),
                    end = point(sang, r - arcWidth),
                    strokeWidth = r * 0.006f,
                )
            }
        }
        val value = gaugeMax * f
        val label = if (gaugeMax >= 100.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
        val lp = point(ang, r - arcWidth * 2.4f)
        drawContext.canvas.nativeCanvas.drawText(
            label, lp.x, lp.y + labelPaint.textSize / 3f, labelPaint,
        )
    }

    // Needle.
    val needleEnd = point(gaugeAngle(fraction), r * 0.88f)
    drawLine(
        color = needleColor,
        start = Offset(cx, cy),
        end = needleEnd,
        strokeWidth = r * 0.035f,
        cap = StrokeCap.Round,
    )
    // Hub.
    drawCircle(color = Color.DarkGray, radius = r * 0.07f, center = Offset(cx, cy))
    drawCircle(color = needleColor, radius = r * 0.035f, center = Offset(cx, cy))
}

private fun formatDose(v: Double): String = when {
    v < 10.0 -> String.format(Locale.US, "%.2f", v)
    v < 100.0 -> String.format(Locale.US, "%.1f", v)
    else -> String.format(Locale.US, "%.0f", v)
}
