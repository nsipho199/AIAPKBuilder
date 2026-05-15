package com.aiapkbuilder.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.AIProvider
import com.aiapkbuilder.app.data.model.AISettings
import com.aiapkbuilder.app.data.model.BuildSettings
import com.aiapkbuilder.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val aiSettings: AISettings = AISettings(),
    val buildSettings: BuildSettings = BuildSettings(),
    val darkMode: Boolean = false,
    val autoBuild: Boolean = true,
    val savedSuccess: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getSettings().collect { settings ->
                _uiState.update { it.copy(
                    aiSettings = settings.first,
                    buildSettings = settings.second,
                    darkMode = settings.third,
                    autoBuild = settings.fourth
                )}
            }
        }
    }

    fun setAIProvider(provider: AIProvider) {
        _uiState.update { it.copy(aiSettings = it.aiSettings.copy(provider = provider)) }
    }
    fun setApiKey(key: String) {
        _uiState.update { it.copy(aiSettings = it.aiSettings.copy(apiKey = key)) }
    }
    fun setAIModel(model: String) {
        _uiState.update { it.copy(aiSettings = it.aiSettings.copy(model = model)) }
    }
    fun setBaseUrl(url: String) {
        _uiState.update { it.copy(aiSettings = it.aiSettings.copy(baseUrl = url)) }
    }
    fun setGithubUsername(username: String) {
        _uiState.update { it.copy(buildSettings = it.buildSettings.copy(githubUsername = username)) }
    }
    fun setGithubToken(token: String) {
        _uiState.update { it.copy(buildSettings = it.buildSettings.copy(githubToken = token)) }
    }
    fun setCodemagicKey(key: String) {
        _uiState.update { it.copy(buildSettings = it.buildSettings.copy(codemagicApiKey = key)) }
    }
    fun setSelfHostedEndpoint(url: String) {
        _uiState.update { it.copy(buildSettings = it.buildSettings.copy(selfHostedEndpoint = url)) }
    }
    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkMode = enabled) }
    }
    fun setAutoBuild(enabled: Boolean) {
        _uiState.update { it.copy(autoBuild = enabled) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.saveSettings(state.aiSettings, state.buildSettings, state.darkMode, state.autoBuild)
            _uiState.update { it.copy(savedSuccess = true) }
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(savedSuccess = false) }
        }
    }
}

// Helper for 4-tuple
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
