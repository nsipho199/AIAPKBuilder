package com.aiapkbuilder.app.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogLine(val text: String)

data class BuildLogUiState(
    val logLines: List<LogLine> = emptyList(),
    val isBuilding: Boolean = false
)

@HiltViewModel
class BuildLogViewModel @Inject constructor(
    private val repository: ProjectRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BuildLogUiState())
    val uiState: StateFlow<BuildLogUiState> = _uiState.asStateFlow()

    fun loadLogs(projectId: String) {
        viewModelScope.launch {
            repository.getProject(projectId).collect { project ->
                project?.let {
                    val lines = it.buildLogs.lines().map { line -> LogLine(line) }
                    _uiState.update { state -> state.copy(
                        logLines = lines,
                        isBuilding = it.buildStatus.name in listOf("BUILDING", "GENERATING")
                    )}
                }
            }
        }
    }

    fun refreshLogs() { /* Re-triggers flow */ }

    fun copyLogs() {
        val text = _uiState.value.logLines.joinToString("\n") { it.text }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Build Logs", text))
    }
}
