package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.DownloadProgress
import com.aiapkbuilder.app.data.model.DownloadSession
import com.aiapkbuilder.app.data.service.export.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadManager: DownloadManager
) : ViewModel() {

    val sessions: StateFlow<List<DownloadSession>> = downloadManager.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = downloadManager.activeDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun pauseDownload(sessionId: String) {
        viewModelScope.launch { downloadManager.pauseDownload(sessionId) }
    }

    fun resumeDownload(sessionId: String) {
        viewModelScope.launch { downloadManager.resumeDownload(sessionId) }
    }

    fun cancelDownload(sessionId: String) {
        viewModelScope.launch { downloadManager.cancelDownload(sessionId) }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            downloadManager.cancelDownload(sessionId)
        }
    }

    fun clearCompleted() {
        viewModelScope.launch { downloadManager.clearCompleted() }
    }
}
