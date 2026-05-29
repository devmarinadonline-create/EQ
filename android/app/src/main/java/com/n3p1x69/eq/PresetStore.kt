package com.n3p1x69.eq

data class Preset(val name: String, val gains: List<Float>)

object PresetStore {
    // 10 values covering sub-bass → treble, scales to any band count
    val presets = listOf(
        Preset("Flat",          listOf( 0f,  0f,  0f,  0f,  0f,  0f,  0f,  0f,  0f,  0f)),
        Preset("Bass Boost",    listOf( 7f,  6f,  4f,  2f,  0f,  0f,  0f,  0f,  0f,  0f)),
        Preset("Bass Reducer",  listOf(-6f, -5f, -3f, -1f,  0f,  0f,  0f,  0f,  0f,  0f)),
        Preset("Treble Boost",  listOf( 0f,  0f,  0f,  0f,  0f,  1f,  2f,  4f,  6f,  7f)),
        Preset("Rock",          listOf( 5f,  4f,  2f,  0f, -1f,  0f,  1f,  3f,  5f,  5f)),
        Preset("Pop",           listOf(-1f,  0f,  2f,  4f,  5f,  4f,  2f,  0f, -1f, -1f)),
        Preset("Classical",     listOf( 0f,  0f,  0f,  0f, -2f, -2f,  0f,  0f,  3f,  4f)),
        Preset("Jazz",          listOf( 4f,  3f,  1f,  0f, -2f, -2f,  0f,  1f,  3f,  4f)),
        Preset("Hip-Hop",       listOf( 8f,  6f,  4f,  1f, -1f, -1f,  0f,  2f,  3f,  4f)),
        Preset("Electronic",    listOf( 7f,  5f,  2f,  0f, -2f,  0f,  0f,  2f,  5f,  6f)),
        Preset("R&B",           listOf( 7f,  5f,  3f,  1f, -1f, -1f,  1f,  2f,  4f,  5f)),
        Preset("Metal",         listOf( 6f,  4f,  2f, -2f, -3f,  0f,  2f,  4f,  5f,  5f)),
        Preset("Acoustic",      listOf( 2f,  2f,  4f,  2f,  1f,  0f,  0f,  1f,  3f,  3f)),
        Preset("Vocal",         listOf(-3f, -3f, -2f,  1f,  4f,  6f,  5f,  3f,  1f,  0f)),
        Preset("Deep Bass",     listOf(10f,  9f,  6f,  3f,  0f,  0f,  0f,  0f,  0f,  0f)),
        Preset("Lounge",        listOf( 2f,  1f,  0f,  0f, -1f,  0f,  1f,  2f,  2f,  2f)),
    )

    fun interpolate(preset: Preset, targetBands: Int): List<Float> {
        val src = preset.gains
        if (src.size == targetBands) return src
        if (targetBands == 1) return listOf(src[src.size / 2])
        return List(targetBands) { i ->
            val t = i.toFloat() / (targetBands - 1)
            val srcPos = t * (src.size - 1)
            val lo = srcPos.toInt().coerceIn(0, src.size - 1)
            val hi = (lo + 1).coerceIn(0, src.size - 1)
            src[lo] + (src[hi] - src[lo]) * (srcPos - lo)
        }
    }
}
