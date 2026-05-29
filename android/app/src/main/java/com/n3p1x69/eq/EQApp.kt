package com.n3p1x69.eq

import android.app.Application

class EQApp : Application() {
    val engine = EQEngine()

    override fun onCreate() {
        super.onCreate()
        PresetStore.load(this)
        engine.init()
    }

    override fun onTerminate() {
        engine.release()
        super.onTerminate()
    }
}
