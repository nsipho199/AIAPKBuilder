package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.StorageStats
import com.aiapkbuilder.app.data.service.export.StorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val storageManager: StorageManager
) : ViewModel() {

    private val _stats = MutableStateFlow<StorageStats?>(null)
    val stats: StateFlow<StorageStats?> = _stats.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    fun loadStats() {
        viewModelScope.launch {
            val result = storageManager.getStorageStats()
            _stats.value = result.getOrNull()
        }
    }

    fun cleanup() {
        viewModelScope.launch {
            _isCleaning.value = true
            storageManager.cleanupOldArtifacts(30)
            storageManager.clearAllExports()
            val result = storageManager.getStorageStats()
            _stats.value = result.getOrNull()
            _isCleaning.value = false
        }
    }

    fun clearAllExports() {
        viewModelScope.launch {
            _isCleaning.value = true
            storageManager.clearAllExports()
            val result = storageManager.getStorageStats()
            _stats.value = result.getOrNull()
            _isCleaning.value = false
        }
    }
}
