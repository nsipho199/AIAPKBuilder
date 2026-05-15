package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentProjects: List<AppProject> = emptyList(),
    val totalProjects: Int = 0,
    val successfulBuilds: Int = 0,
    val activeBuilds: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                _uiState.update { state ->
                    state.copy(
                        recentProjects = projects.take(10),
                        totalProjects = projects.size,
                        successfulBuilds = projects.count { it.buildStatus.name == "SUCCESS" },
                        activeBuilds = projects.count {
                            it.buildStatus.name in listOf("BUILDING", "GENERATING")
                        }
                    )
                }
            }
        }
    }
}
