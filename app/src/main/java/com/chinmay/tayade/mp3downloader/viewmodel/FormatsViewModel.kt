package com.chinmay.tayade.mp3downloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.tayade.mp3downloader.data.DownloadRepository
import com.chinmay.tayade.mp3downloader.data.PythonDownloader
import com.chinmay.tayade.mp3downloader.data.SettingsStore
import com.chinmay.tayade.mp3downloader.data.model.DownloadRequest
import com.chinmay.tayade.mp3downloader.data.model.FormatOptions
import com.chinmay.tayade.mp3downloader.data.model.MediaSelection
import com.chinmay.tayade.mp3downloader.util.StoragePaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FormatsViewModel(app: Application) : AndroidViewModel(app) {

    sealed interface UiState {
        object Loading : UiState
        data class Error(val message: String) : UiState
        data class Ready(val options: FormatOptions) : UiState
    }

    private val repo = DownloadRepository.get(app)
    private val settingsStore = SettingsStore(app)

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state = _state.asStateFlow()

    private val _enqueued = MutableStateFlow(false)
    val enqueued = _enqueued.asStateFlow()

    private var loadedUrl: String? = null

    fun load(url: String) {
        if (loadedUrl == url && _state.value !is UiState.Error) return
        loadedUrl = url
        _state.value = UiState.Loading
        viewModelScope.launch {
            val preferredAudio = settingsStore.settings.first().preferredAudioExt
            val options = withContext(Dispatchers.IO) { PythonDownloader.probeFormats(url) }
            _state.value = if (options.info.ok) {
                val reordered = options.audioExtensions
                    .sortedByDescending { it.equals(preferredAudio, ignoreCase = true) }
                UiState.Ready(options.copy(audioExtensions = reordered))
            } else {
                UiState.Error(options.info.error.ifBlank { "Couldn't read this video" })
            }
        }
    }

    fun retry() {
        loadedUrl?.let {
            loadedUrl = null
            load(it)
        }
    }

    fun startDownload(url: String, selection: MediaSelection) {
        val ready = _state.value as? UiState.Ready ?: return
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            val dir = settings.downloadDir.takeIf { StoragePaths.isWritable(it) }
                ?: StoragePaths.appPrivateDownloadDir(getApplication())
            repo.enqueue(
                DownloadRequest(
                    url = url,
                    title = ready.options.info.title,
                    thumbnail = ready.options.info.thumbnail,
                    destinationDir = dir,
                    selection = selection,
                ),
            )
            _enqueued.value = true
        }
    }
}
