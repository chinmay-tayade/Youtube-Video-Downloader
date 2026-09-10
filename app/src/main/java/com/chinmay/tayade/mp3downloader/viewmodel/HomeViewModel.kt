package com.chinmay.tayade.mp3downloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.tayade.mp3downloader.data.AppSettings
import com.chinmay.tayade.mp3downloader.data.SettingsStore
import com.chinmay.tayade.mp3downloader.util.StoragePaths
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsStore = SettingsStore(app)

    val settings: StateFlow<AppSettings?> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setDownloadDir(path: String) {
        val cleaned = path.trim().ifBlank { return }
        viewModelScope.launch { settingsStore.setDownloadDir(cleaned) }
    }

    fun resetToDefaultDir() {
        viewModelScope.launch {
            settingsStore.setDownloadDir(StoragePaths.defaultDownloadDir(getApplication()))
        }
    }
}
