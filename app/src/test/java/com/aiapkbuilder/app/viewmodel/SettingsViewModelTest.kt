package com.aiapkbuilder.app.viewmodel

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.data.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SettingsViewModelTest {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        repository = mockk()
        viewModel = SettingsViewModel(repository)
    }

    @Test
    fun `initial state has default settings`() {
        val state = viewModel.uiState.value
        assertEquals(AIProvider.OPENAI, state.aiSettings.provider)
        assertEquals(BuildProvider.GITHUB_ACTIONS, state.buildSettings.defaultProvider)
        assertFalse(state.darkMode)
    }

    @Test
    fun `updateAIApiKey updates state`() {
        viewModel.updateAIApiKey("sk-test")
        assertEquals("sk-test", viewModel.uiState.value.aiSettings.apiKey)
    }

    @Test
    fun `updateAIModel updates state`() {
        viewModel.updateAIModel("gpt-4-turbo")
        assertEquals("gpt-4-turbo", viewModel.uiState.value.aiSettings.model)
    }

    @Test
    fun `updateAIProvider updates state`() {
        viewModel.updateAIProvider(AIProvider.OLLAMA)
        assertEquals(AIProvider.OLLAMA, viewModel.uiState.value.aiSettings.provider)
    }

    @Test
    fun `toggleDarkMode toggles state`() {
        viewModel.toggleDarkMode()
        assertTrue(viewModel.uiState.value.darkMode)
        viewModel.toggleDarkMode()
        assertFalse(viewModel.uiState.value.darkMode)
    }

    @Test
    fun `saveSettings calls repository`() = runTest {
        coEvery { repository.saveSettings(any(), any(), any(), any()) } returns Unit

        viewModel.updateAIApiKey("sk-test")
        viewModel.saveSettings()

        coVerify { repository.saveSettings(any(), any(), any(), any()) }
    }
}
