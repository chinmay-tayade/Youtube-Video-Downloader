package com.chinmay.tayade.mp3downloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chinmay.tayade.mp3downloader.data.SettingsStore
import com.chinmay.tayade.mp3downloader.data.ThemeMode
import com.chinmay.tayade.mp3downloader.ui.navigation.AppNavHost
import com.chinmay.tayade.mp3downloader.ui.theme.Mp3DownloaderTheme
import com.chinmay.tayade.mp3downloader.util.isWebUrl
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val startUrl = MutableStateFlow<String?>(null)
    private val openDownloads = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        handleIntent(intent)

        val settingsStore = SettingsStore(applicationContext)

        setContent {
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val seedUrl by startUrl.collectAsStateWithLifecycle()
            val toDownloads by openDownloads.collectAsStateWithLifecycle()

            Mp3DownloaderTheme(darkTheme = dark) {
                AppNavHost(
                    startUrl = seedUrl,
                    openDownloadsOnLaunch = toDownloads,
                    onDownloadsShortcutConsumed = { openDownloads.value = false },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        if (intent.getBooleanExtra(EXTRA_OPEN_DOWNLOADS, false)) {
            openDownloads.value = true
        }
        val shared = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }
        firstUrlIn(shared)?.let { startUrl.value = it }
    }

    private fun firstUrlIn(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val token = text.split(Regex("\\s+")).firstOrNull { isWebUrl(it) }
        return token ?: text.trim().takeIf { isWebUrl(it) }
    }

    companion object {
        const val EXTRA_OPEN_DOWNLOADS = "open_downloads"
    }
}
