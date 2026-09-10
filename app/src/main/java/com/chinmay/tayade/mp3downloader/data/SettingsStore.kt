package com.chinmay.tayade.mp3downloader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.chinmay.tayade.mp3downloader.util.StoragePaths
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val downloadDir: String,
    val themeMode: ThemeMode,
    val preferredAudioExt: String,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App-wide preferences, backed by DataStore. */
class SettingsStore(private val context: Context) {

    private object Keys {
        val DOWNLOAD_DIR = stringPreferencesKey("download_dir")
        val THEME = stringPreferencesKey("theme_mode")
        val AUDIO_EXT = stringPreferencesKey("preferred_audio_ext")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            downloadDir = prefs[Keys.DOWNLOAD_DIR]
                ?: StoragePaths.defaultDownloadDir(context),
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            preferredAudioExt = prefs[Keys.AUDIO_EXT] ?: "m4a",
        )
    }

    suspend fun setDownloadDir(path: String) =
        context.dataStore.edit { it[Keys.DOWNLOAD_DIR] = path }

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setPreferredAudioExt(ext: String) =
        context.dataStore.edit { it[Keys.AUDIO_EXT] = ext }
}
