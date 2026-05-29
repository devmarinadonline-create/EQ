package com.n3p1x69.eq

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class EQViewModel(val engine: EQEngine) : ViewModel() {

    var selectedPreset by mutableStateOf<String?>(null)
        private set

    fun applyPreset(preset: Preset) {
        val gains = PresetStore.interpolate(preset, engine.bands.value.size)
        engine.applyGains(gains)
        selectedPreset = preset.name
    }

    fun reset() {
        engine.reset()
        selectedPreset = null
    }

    fun setBand(index: Int, gain: Float) {
        engine.setBand(index, gain)
        selectedPreset = null
    }
}

class EQViewModelFactory(private val engine: EQEngine) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = EQViewModel(engine) as T
}
