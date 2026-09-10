package com.chinmay.tayade.mp3downloader

import android.app.Application
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chinmay.tayade.mp3downloader.data.DownloadRepository
import com.chinmay.tayade.mp3downloader.data.service.NotificationHelper

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

        NotificationHelper.ensureChannels(this)
        // Warm the singleton so Room is ready and stale RUNNING rows get recovered.
        DownloadRepository.get(this)
    }
}
