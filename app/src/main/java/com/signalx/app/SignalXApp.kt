package com.signalx.app

import android.app.Application
import com.signalx.app.ads.AdManager

class SignalXApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AdManager.initialize(this)
    }
}

