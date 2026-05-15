package com.aiapkbuilder.app.viewmodel

import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.repository.ProjectRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: ProjectRepository

    @Before
    fun setup() {
        repository = mockk()
    }

    @Test
    fun `uiState updates with project stats`() = runTest {
        val projects = listOf(
            AppProject(id = "1", name = "A", description = "d", prompt = "p", appType = AppType.CUSTOM, buildStatus = BuildStatus.SUCCESS),
            AppProject(id = "2", name = "B", description = "d", prompt = "p", appType = AppType.CUSTOM, buildStatus = BuildStatus.BUILDING),
            AppProject(id = "3", name = "C", description = "d", prompt = "p", appType = AppType.CUSTOM, buildStatus = BuildStatus.SUCCESS)
        )
        coEvery { repository.getAllProjects() } returns MutableStateFlow(projects)

        viewModel = HomeViewModel(repository)

        val state = viewModel.uiState.value
        assertEquals(3, state.totalProjects)
        assertEquals(2, state.successfulBuilds)
        assertEquals(1, state.activeBuilds)
    }

    @Test
    fun `homeUiState starts empty`() {
        coEvery { repository.getAllProjects() } returns MutableStateFlow(emptyList())
        viewModel = HomeViewModel(repository)
        val state = viewModel.uiState.value
        assertTrue(state.recentProjects.isEmpty())
        assertEquals(0, state.totalProjects)
    }
}
