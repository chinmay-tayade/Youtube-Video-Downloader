package com.chinmay.tayade.mp3downloader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chinmay.tayade.mp3downloader.data.DownloadRepository
import com.chinmay.tayade.mp3downloader.data.model.DownloadRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = DownloadRepository.get(app)

    val records: StateFlow<List<DownloadRecord>> = repo.records
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun cancel(id: Long) = repo.cancel(id)

    fun retry(id: Long) {
        viewModelScope.launch { repo.retry(id) }
    }

    fun delete(id: Long, alsoRemoveFile: Boolean) {
        viewModelScope.launch { repo.delete(id, alsoRemoveFile) }
    }

    fun clearFinished() {
        viewModelScope.launch { repo.clearFinished() }
    }
}
