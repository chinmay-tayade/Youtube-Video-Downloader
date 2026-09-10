package com.chinmay.tayade.mp3downloader

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
            }
        } catch (e: Exception) {
            Log.e("App", "Unable to start Python runtime", e)
        }
    }
}
