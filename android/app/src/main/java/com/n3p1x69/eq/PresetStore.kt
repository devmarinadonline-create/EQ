package com.n3p1x69.eq

import android.content.Context
import org.json.JSONObject
import kotlin.math.log10

data class Preset(
    val name: String,
    val frequencies: List<Float>,
    val gains: List<Float>
)

object PresetStore {
    private var _presets = listOf<Preset>()
    val presets get() = _presets

    fun load(context: Context) {
        try {
            val json = context.assets.open("presets.json").bufferedReader().readText()
            val obj = JSONObject(json)
            _presets = obj.keys().asSequence().map { key ->
                val p = obj.getJSONObject(key)
                val freqs = p.getJSONArray("frequencies")
                val gains = p.getJSONArray("gains")
                Preset(
                    name = key,
                    frequencies = List(freqs.length()) { freqs.getDouble(it).toFloat() },
                    gains = List(gains.length()) { gains.getDouble(it).toFloat() }
                )
            }.sortedBy { it.name.lowercase() }.toList()
        } catch (_: Exception) {
            _presets = emptyList()
        }
    }

    // Map preset gains to device EQ bands using log-frequency interpolation
    fun mapToDeviceBands(preset: Preset, deviceBands: List<Band>): List<Float> {
        val sorted = preset.frequencies.zip(preset.gains)
            .filter { it.first > 1f }
            .sortedBy { it.first }
        if (sorted.isEmpty()) return deviceBands.map { 0f }
        return deviceBands.map { band -> interpolateLog(band.centerFreqHz, sorted) }
    }

    // Fixed 10-point preview for mini EQ bar (sorted by freq, resampled)
    fun previewGains(preset: Preset, count: Int = 10): List<Float> {
        val sorted = preset.frequencies.zip(preset.gains)
            .filter { it.first > 1f }
            .sortedBy { it.first }
            .map { it.second }
        if (sorted.isEmpty()) return List(count) { 0f }
        if (sorted.size == count) return sorted
        return List(count) { i ->
            val t = if (count == 1) 0f else i.toFloat() / (count - 1)
            val pos = t * (sorted.size - 1)
            val lo = pos.toInt().coerceIn(0, sorted.size - 1)
            val hi = (lo + 1).coerceIn(0, sorted.size - 1)
            sorted[lo] + (sorted[hi] - sorted[lo]) * (pos - lo)
        }
    }

    private fun interpolateLog(targetFreq: Float, sorted: List<Pair<Float, Float>>): Float {
        if (targetFreq <= sorted.first().first) return sorted.first().second
        if (targetFreq >= sorted.last().first) return sorted.last().second
        val idx = sorted.indexOfFirst { it.first >= targetFreq }.coerceAtLeast(1)
        val (loF, loG) = sorted[idx - 1]
        val (hiF, hiG) = sorted[idx]
        val t = ((log10(targetFreq) - log10(loF)) / (log10(hiF) - log10(loF))).coerceIn(0f, 1f)
        return loG + (hiG - loG) * t
    }
}
