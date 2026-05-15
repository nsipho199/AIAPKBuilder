package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<AppProject> = emptyList(),
    val selectedFilter: BuildStatus? = null
) {
    val filteredProjects: List<AppProject>
        get() = if (selectedFilter == null) projects
                else projects.filter { it.buildStatus == selectedFilter }
}

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectsUiState())
    val uiState: StateFlow<ProjectsUiState> = _uiState.asStateFlow()

    init { refreshProjects() }

    fun refreshProjects() {
        viewModelScope.launch {
            repository.getAllProjects().collect { projects ->
                _uiState.update { it.copy(projects = projects) }
            }
        }
    }

    fun setFilter(status: BuildStatus?) {
        _uiState.update { it.copy(selectedFilter = status) }
    }
}
