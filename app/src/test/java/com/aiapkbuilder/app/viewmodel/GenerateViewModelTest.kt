package com.aiapkbuilder.app.viewmodel

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.service.build.BuildExecutor
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GenerateViewModelTest {

    private lateinit var viewModel: GenerateViewModel
    private lateinit var repository: ProjectRepository
    private lateinit var buildExecutor: BuildExecutor

    @Before
    fun setup() {
        repository = mockk()
        buildExecutor = mockk()
        viewModel = GenerateViewModel(repository, buildExecutor)
    }

    @Test
    fun `initial state has defaults`() {
        val state = viewModel.uiState.value
        assertEquals("", state.prompt)
        assertEquals(AppType.CUSTOM, state.selectedAppType)
        assertEquals(BuildProvider.GITHUB_ACTIONS, state.selectedProvider)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `onPromptChange updates prompt`() {
        viewModel.onPromptChange("Build a weather app")
        assertEquals("Build a weather app", viewModel.uiState.value.prompt)
    }

    @Test
    fun `onPromptChange clears error`() {
        viewModel.onPromptChange("")
        viewModel.generateApp()
        assertNotNull(viewModel.uiState.value.error)
        viewModel.onPromptChange("New prompt")
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun `onAppTypeSelected updates type`() {
        viewModel.onAppTypeSelected(AppType.CHAT)
        assertEquals(AppType.CHAT, viewModel.uiState.value.selectedAppType)
    }

    @Test
    fun `onProviderSelected updates provider`() {
        viewModel.onProviderSelected(BuildProvider.DOCKER)
        assertEquals(BuildProvider.DOCKER, viewModel.uiState.value.selectedProvider)
    }

    @Test
    fun `onFeatureToggled adds and removes features`() {
        viewModel.onFeatureToggled("Dark Mode")
        assertTrue(viewModel.uiState.value.selectedFeatures.contains("Dark Mode"))
        viewModel.onFeatureToggled("Dark Mode")
        assertFalse(viewModel.uiState.value.selectedFeatures.contains("Dark Mode"))
    }

    @Test
    fun `generateApp with blank prompt shows error`() {
        viewModel.generateApp()
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `generatePreview with blank prompt does nothing`() {
        viewModel.generatePreview()
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `clearCreatedProject resets id`() {
        viewModel.clearCreatedProject()
        assertNull(viewModel.uiState.value.createdProjectId)
    }

    @Test
    fun `generateApp with valid prompt succeeds`() = runTest {
        val projectId = "test_project_123"
        val plan = GeneratedProjectPlan(
            appName = "Weather App", packageName = "com.weather", description = "A weather app",
            appType = AppType.WEATHER, screens = emptyList(), features = listOf("Forecast"),
            dependencies = listOf("compose"), colorPrimary = "#6200EA", colorSecondary = "#03DAC6",
            minSdkVersion = 26, permissions = listOf("INTERNET"), navigationStructure = "Bottom",
            databaseTables = emptyList(), apiEndpoints = emptyList()
        )

        coEvery { repository.generateProjectPlan(any()) } returns plan
        coEvery { repository.generateAndCreateProject(any(), any(), any()) } returns projectId
        coEvery { buildExecutor.executeBuild(any(), any(), any()) } returns Result.success(
            BuildJob(jobId = "j1", projectId = projectId, provider = BuildProvider.GITHUB_ACTIONS, status = BuildStatus.SUCCESS)
        )

        viewModel.onPromptChange("Build a weather app")
        viewModel.generateApp()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }
}
