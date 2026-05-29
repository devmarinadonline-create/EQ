package com.n3p1x69.eq.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.n3p1x69.eq.*
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.pow

private val BG = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1A1A1A)
private val Green = Color(0xFF00E676)
private val GreenSelected = Color(0xFF00C853)
private val Orange = Color(0xFFFF6D00)

@Composable
fun EQTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(background = BG, surface = CardBg, primary = Green),
        content = content
    )
}

@Composable
fun EQScreen(vm: EQViewModel) {
    val bands by vm.engine.bands.collectAsState()
    val supported by vm.engine.supported.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BG)
            .systemBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("EQ", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
                vm.selectedPreset?.let {
                    Text(it, fontSize = 11.sp, color = Green)
                }
            }
            TextButton(onClick = vm::reset) {
                Text("Reset", color = Color.Gray, fontSize = 13.sp)
            }
        }

        if (!supported) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(160.dp)
                    .background(CardBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("System EQ not supported on this device", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            EQCurve(
                bands = bands,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Volume boost
        val loudness by vm.engine.loudnessDb.collectAsState()
        LoudnessRow(loudnessDb = loudness, onChange = vm::setLoudness)

        Spacer(Modifier.height(4.dp))

        // Tab switcher
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1C1C1C))
        ) {
            listOf("Presets", "Bands").forEachIndexed { i, label ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { tab = i }
                        .background(if (tab == i) Color(0xFF2E2E2E) else Color.Transparent)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        label,
                        color = if (tab == i) Color.White else Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = if (tab == i) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        if (tab == 0) {
            PresetGrid(
                presets = PresetStore.presets,
                selectedName = vm.selectedPreset,
                onSelect = vm::applyPreset,
                modifier = Modifier.weight(1f)
            )
        } else {
            BandSliders(
                bands = bands,
                onBandChange = vm::setBand,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LoudnessRow(loudnessDb: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Громкость", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.width(72.dp))
        Slider(
            value = loudnessDb,
            onValueChange = onChange,
            valueRange = 0f..20f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Green,
                activeTrackColor = Green,
                inactiveTrackColor = Color(0xFF2E2E2E)
            )
        )
        Text(
            text = "+${loudnessDb.toInt()} dB",
            fontSize = 12.sp,
            color = if (loudnessDb > 0f) Green else Color.Gray,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}

// Gaussian frequency response simulation (same as iOS app)
private fun responseAt(freqHz: Float, bands: List<Band>): Float {
    val sigma = 0.85f
    var sum = 0f
    for (band in bands) {
        val f0 = band.centerFreqHz.coerceAtLeast(1f)
        val d = log2(freqHz / f0) / sigma
        sum += band.gainDb * exp(-0.5f * d * d)
    }
    return sum.coerceIn(-30f, 30f)
}

@Composable
fun EQCurve(bands: List<Band>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141414))
    ) {
        if (bands.isEmpty()) return@Canvas

        val minLog = log10(20f)
        val maxLog = log10(20000f)
        val maxDb = bands.first().maxDb.coerceAtLeast(12f)

        fun freqToX(f: Float) = ((log10(f.coerceAtLeast(1f)) - minLog) / (maxLog - minLog) * size.width)
        fun gainToY(g: Float) = (1f - (g + maxDb) / (2 * maxDb)) * size.height

        // Grid lines
        listOf(-10f, 0f, 10f).forEach { g ->
            val y = gainToY(g)
            drawLine(
                color = if (g == 0f) Color.White.copy(0.3f) else Color.White.copy(0.1f),
                start = Offset(0f, y), end = Offset(size.width, y),
                strokeWidth = if (g == 0f) 1.5f else 0.8f
            )
        }
        listOf(100f, 1000f, 10000f).forEach { f ->
            drawLine(Color.White.copy(0.1f), Offset(freqToX(f), 0f), Offset(freqToX(f), size.height), 0.8f)
        }

        // Sample 200 points for smooth curve
        val steps = 200
        val pts = (0..steps).map { i ->
            val t = i.toFloat() / steps
            val logF = minLog + t * (maxLog - minLog)
            val freq = 10.0.pow(logF.toDouble()).toFloat()
            val gain = responseAt(freq, bands)
            Offset(t * size.width, gainToY(gain))
        }

        val zeroY = gainToY(0f)

        // Fill under curve
        val fill = Path().apply {
            moveTo(pts.first().x, zeroY)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, zeroY)
            close()
        }
        drawPath(fill, Brush.verticalGradient(
            listOf(Green.copy(0.35f), Color.Transparent), 0f, size.height
        ))

        // Smooth curve line
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, Green, style = Stroke(2.5f))

        // Band dots at actual center frequencies
        bands.forEach { band ->
            val pt = Offset(freqToX(band.centerFreqHz), gainToY(band.gainDb))
            drawCircle(Color.White, 5f, pt)
            drawCircle(Green, 5f, pt, style = Stroke(1.5f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetGrid(
    presets: List<Preset>,
    selectedName: String?,
    onSelect: (Preset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        FlowRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            presets.forEach { preset ->
                val selected = preset.name == selectedName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selected) GreenSelected else Color(0xFF262626))
                        .clickable { onSelect(preset) }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        preset.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.Black else Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MiniEQBar(gains: List<Float>, inverted: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (gains.isEmpty()) return@Canvas
        val bw = size.width / gains.size
        val mid = size.height / 2f
        gains.forEachIndexed { i, g ->
            val h = (g.coerceIn(-30f, 30f) / 30f * mid).coerceAtLeast(0f)
            val color = when {
                inverted && g >= 0 -> Color.Black.copy(0.55f)
                inverted -> Color.Black.copy(0.35f)
                g >= 0 -> Green.copy(0.85f)
                else -> Orange.copy(0.75f)
            }
            drawRect(color,
                topLeft = Offset(i * bw + 0.5f, if (g >= 0) mid - h else mid),
                size = Size((bw - 1f).coerceAtLeast(1f), h.coerceAtLeast(1f))
            )
        }
        drawLine(Color.White.copy(if (inverted) 0.3f else 0.2f), Offset(0f, mid), Offset(size.width, mid), 0.5f)
    }
}

@Composable
fun BandSliders(bands: List<Band>, onBandChange: (Int, Float) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bands.forEach { band ->
            BandSlider(band = band, onChange = { onBandChange(band.index, it) })
        }
    }
}

@Composable
fun BandSlider(band: Band, onChange: (Float) -> Unit) {
    val range = band.maxDb - band.minDb
    var startGain by remember(band.index) { mutableFloatStateOf(0f) }
    var accumulated by remember(band.index) { mutableFloatStateOf(0f) }

    val freqLabel = if (band.centerFreqHz >= 1000f) {
        val k = band.centerFreqHz / 1000f
        if (k % 1f == 0f) "${k.toInt()}k" else "${"%.1f".format(k)}k"
    } else "${band.centerFreqHz.toInt()}"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(40.dp)
    ) {
        Text(
            text = if (band.gainDb >= 0f) "+${band.gainDb.toInt()}" else "${band.gainDb.toInt()}",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                band.gainDb > 0 -> Green
                band.gainDb < 0 -> Orange
                else -> Color.Gray
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp, 140.dp)
                .pointerInput(band.index) {
                    detectDragGestures(
                        onDragStart = { _ ->
                            startGain = band.gainDb
                            accumulated = 0f
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            accumulated += drag.y
                            val newGain = (startGain - accumulated / 140f * range)
                                .coerceIn(band.minDb, band.maxDb)
                            onChange(newGain)
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val normalized = (band.gainDb - band.minDb) / range
                val thumbY = (1f - normalized) * size.height

                // Track
                drawRoundRect(
                    color = Color(0xFF2A2A2A),
                    topLeft = Offset(cx - 2f, 0f),
                    size = Size(4f, size.height),
                    cornerRadius = CornerRadius(2f)
                )

                // Zero tick
                drawLine(Color.White.copy(0.5f), Offset(cx - 7f, cy), Offset(cx + 7f, cy), 1f)

                // Fill
                if (band.gainDb >= 0) {
                    drawRect(Green, topLeft = Offset(cx - 2f, thumbY), size = Size(4f, cy - thumbY))
                } else {
                    drawRect(Orange, topLeft = Offset(cx - 2f, cy), size = Size(4f, thumbY - cy))
                }

                // Thumb
                drawCircle(Color.White, radius = 10f, center = Offset(cx, thumbY))
                drawCircle(
                    color = if (band.gainDb != 0f) Green else Color(0xFF444444),
                    radius = 10f, center = Offset(cx, thumbY), style = Stroke(1.5f)
                )
            }
        }

        Text(
            freqLabel,
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
