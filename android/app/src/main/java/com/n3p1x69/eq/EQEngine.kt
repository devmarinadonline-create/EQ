package com.n3p1x69.eq

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Band(
    val index: Int,
    val centerFreqHz: Float,
    val gainDb: Float,
    val minDb: Float,
    val maxDb: Float
)

class EQEngine {
    private var eq: Equalizer? = null

    private val _bands = MutableStateFlow<List<Band>>(emptyList())
    val bands: StateFlow<List<Band>> = _bands.asStateFlow()

    private val _supported = MutableStateFlow(true)
    val supported: StateFlow<Boolean> = _supported.asStateFlow()

    fun init() {
        try {
            eq = Equalizer(0, 0).apply { enabled = true }
            _supported.value = true
            refreshBands()
        } catch (e: Exception) {
            _supported.value = false
        }
    }

    private fun refreshBands() {
        val e = eq ?: return
        val range = e.bandLevelRange
        val minDb = range[0] / 100f
        val maxDb = range[1] / 100f
        _bands.value = (0 until e.numberOfBands.toInt()).map { i ->
            Band(
                index = i,
                centerFreqHz = e.getCenterFreq(i.toShort()) / 1000f,
                gainDb = e.getBandLevel(i.toShort()) / 100f,
                minDb = minDb,
                maxDb = maxDb
            )
        }
    }

    fun setBand(index: Int, gainDb: Float) {
        val e = eq ?: return
        try {
            e.setBandLevel(index.toShort(), (gainDb * 100).toInt().toShort())
            _bands.update { list ->
                list.toMutableList().also {
                    if (index < it.size) it[index] = it[index].copy(gainDb = gainDb)
                }
            }
        } catch (_: Exception) {}
    }

    fun applyGains(gains: List<Float>) {
        val count = eq?.numberOfBands?.toInt() ?: return
        for (i in 0 until count) {
            setBand(i, gains.getOrElse(i) { 0f })
        }
    }

    fun reset() {
        val count = eq?.numberOfBands?.toInt() ?: return
        for (i in 0 until count) setBand(i, 0f)
    }

    fun release() {
        eq?.release()
        eq = null
    }
}
