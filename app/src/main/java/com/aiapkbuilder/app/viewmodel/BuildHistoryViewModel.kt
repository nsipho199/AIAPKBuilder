package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.BuildHistory
import com.aiapkbuilder.app.data.service.export.BuildHistoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuildHistoryViewModel @Inject constructor(
    private val historyManager: BuildHistoryManager
) : ViewModel() {

    private val _history = MutableStateFlow<List<BuildHistory>>(emptyList())
    val history: StateFlow<List<BuildHistory>> = _history.asStateFlow()

    private val _stats = MutableStateFlow<Map<String, Any>>(emptyMap())
    val stats: StateFlow<Map<String, Any>> = _stats.asStateFlow()

    fun loadHistory(projectId: String) {
        viewModelScope.launch {
            historyManager.getHistoryForProject(projectId).collect { list ->
                _history.value = list
            }
        }
        viewModelScope.launch {
            _stats.value = historyManager.getBuildHistoryStats(projectId)
        }
    }

    fun deleteHistory(historyId: String) {
        viewModelScope.launch {
            historyManager.deleteHistory(historyId)
        }
    }

    fun clearProjectHistory(projectId: String) {
        viewModelScope.launch {
            historyManager.clearProjectHistory(projectId)
        }
    }
}
