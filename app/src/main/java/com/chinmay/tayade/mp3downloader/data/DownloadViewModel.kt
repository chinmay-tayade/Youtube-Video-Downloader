package com.chinmay.tayade.mp3downloader.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadUiState(
    val loadingInfo: Boolean = true,
    val info: VideoInfo? = null,
    val statusText: String = "Grabbing info…",
    val inProgress: Boolean = true,
    val finished: Boolean = false,
    val success: Boolean = false,
    val fatalError: String? = null,
)

class DownloadViewModel : ViewModel() {

    private val _state = MutableStateFlow(DownloadUiState())
    val state = _state.asStateFlow()

    private var started = false

    fun start(link: String, destination: String) {
        if (started) return
        started = true

        viewModelScope.launch {
            val info = withContext(Dispatchers.IO) { PythonDownloader.getVideoInfo(link) }

            if (!info.ok) {
                _state.update {
                    it.copy(
                        loadingInfo = false,
                        inProgress = false,
                        finished = true,
                        fatalError = info.error.ifBlank { "Could not load video information" },
                        statusText = "Couldn't load video",
                    )
                }
                return@launch
            }

            _state.update {
                it.copy(loadingInfo = false, info = info, statusText = "Downloading…")
            }

            val result = withContext(Dispatchers.IO) {
                PythonDownloader.downloadVideo(link, destination)
            }

            _state.update {
                it.copy(
                    inProgress = false,
                    finished = true,
                    success = result.ok,
                    statusText = result.message.ifBlank {
                        if (result.ok) "Download complete" else "Download failed"
                    },
                )
            }
        }
    }
}
