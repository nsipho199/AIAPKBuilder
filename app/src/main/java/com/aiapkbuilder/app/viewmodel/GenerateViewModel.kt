package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.GenerationRequest
import com.aiapkbuilder.app.data.model.GeneratedProjectPlan
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.service.build.BuildExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenerateUiState(
    val prompt: String = "",
    val selectedAppType: AppType = AppType.CUSTOM,
    val selectedProvider: BuildProvider = BuildProvider.GITHUB_ACTIONS,
    val selectedFeatures: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val loadingMessage: String = "Analyzing your idea...",
    val progress: Float = 0f,
    val error: String? = null,
    val createdProjectId: String? = null,
    val previewPlan: GeneratedProjectPlan? = null
)

@HiltViewModel
class GenerateViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val buildExecutor: BuildExecutor
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    private val _previewPlan = MutableStateFlow<GeneratedProjectPlan?>(null)
    val previewPlan: StateFlow<GeneratedProjectPlan?> = _previewPlan.asStateFlow()

    fun onPromptChange(value: String) {
        _uiState.update { it.copy(prompt = value, error = null) }
    }

    fun onAppTypeSelected(type: AppType) {
        _uiState.update { it.copy(selectedAppType = type) }
    }

    fun onProviderSelected(provider: BuildProvider) {
        _uiState.update { it.copy(selectedProvider = provider) }
    }

    fun onFeatureToggled(feature: String) {
        _uiState.update { state ->
            val features = state.selectedFeatures.toMutableSet()
            if (feature in features) features.remove(feature) else features.add(feature)
            state.copy(selectedFeatures = features)
        }
    }

    fun generateApp() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.update { it.copy(error = "Please enter an app description") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null, progress = 0.05f, loadingMessage = "Analyzing your idea...") }

                val steps = listOf(
                    0.1f to "Planning app structure...",
                    0.25f to "Generating screens and navigation...",
                    0.45f to "Creating UI components...",
                    0.6f to "Configuring features and APIs...",
                    0.75f to "Writing database schemas...",
                    0.88f to "Setting up build pipeline...",
                    0.95f to "Launching build..."
                )

                val request = GenerationRequest(
                    prompt = prompt,
                    appType = _uiState.value.selectedAppType,
                    additionalFeatures = _uiState.value.selectedFeatures.toList()
                )

                val projectId = repository.generateAndCreateProject(
                    request = request,
                    buildProvider = _uiState.value.selectedProvider,
                    onProgress = { step, message ->
                        val (prog, msg) = steps.getOrElse(step) { steps.last() }
                        _uiState.update { it.copy(progress = prog, loadingMessage = msg) }
                    }
                )

                // After code generation, trigger the build
                _uiState.update { it.copy(progress = 0.95f, loadingMessage = "Starting build...") }

                val buildResult = buildExecutor.executeBuild(
                    projectId = projectId,
                    preferredProvider = _uiState.value.selectedProvider,
                    onProgress = { percent, message ->
                        val progress = 0.95f + (percent / 100f * 0.05f) // 95-100% for build
                        _uiState.update { it.copy(progress = progress, loadingMessage = message) }
                    }
                )

                if (buildResult.isFailure) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = buildResult.exceptionOrNull()?.message ?: "Build failed",
                        progress = 0f
                    )}
                } else {
                    _uiState.update { it.copy(isLoading = false, progress = 1f, createdProjectId = projectId) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Generation failed. Check your AI settings.",
                    progress = 0f
                )}
            }
        }
    }

    fun generatePreview() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) return

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                _previewPlan.value = null

                val request = GenerationRequest(
                    prompt = prompt,
                    appType = _uiState.value.selectedAppType,
                    additionalFeatures = _uiState.value.selectedFeatures.toList()
                )

                val plan = repository.generateProjectPlan(request)
                _previewPlan.value = plan

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Preview generation failed"
                )}
            }
        }
    }

    fun clearCreatedProject() {
        _uiState.update { it.copy(createdProjectId = null) }
    }
}