package com.chinmay.tayade.mp3downloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.tayade.mp3downloader.data.AppSettings
import com.chinmay.tayade.mp3downloader.data.SettingsStore
import com.chinmay.tayade.mp3downloader.data.ThemeMode
import com.chinmay.tayade.mp3downloader.util.StoragePaths
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val store = SettingsStore(app)

    val settings: StateFlow<AppSettings?> = store.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode) }

    fun setAudioExt(ext: String) = viewModelScope.launch { store.setPreferredAudioExt(ext) }

    fun setDownloadDir(path: String) = viewModelScope.launch { store.setDownloadDir(path) }

    fun resetDownloadDir() = viewModelScope.launch {
        store.setDownloadDir(StoragePaths.defaultDownloadDir(getApplication()))
    }
}
