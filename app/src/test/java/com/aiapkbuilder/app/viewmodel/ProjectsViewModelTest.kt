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

class ProjectsViewModelTest {

    private lateinit var viewModel: ProjectsViewModel
    private lateinit var repository: ProjectRepository

    @Before
    fun setup() {
        repository = mockk()
    }

    @Test
    fun `projects list loads on init`() = runTest {
        val projects = listOf(
            AppProject(id = "1", name = "A", description = "d", prompt = "p", appType = AppType.CALCULATOR, buildStatus = BuildStatus.SUCCESS),
            AppProject(id = "2", name = "B", description = "d", prompt = "p", appType = AppType.WEATHER, buildStatus = BuildStatus.BUILDING)
        )
        coEvery { repository.getAllProjects() } returns MutableStateFlow(projects)
        viewModel = ProjectsViewModel(repository)
        assertEquals(2, viewModel.uiState.value.projects.size)
    }

    @Test
    fun `setFilter filters projects`() = runTest {
        val projects = listOf(
            AppProject(id = "1", name = "A", description = "d", prompt = "p", appType = AppType.CALCULATOR, buildStatus = BuildStatus.SUCCESS),
            AppProject(id = "2", name = "B", description = "d", prompt = "p", appType = AppType.WEATHER, buildStatus = BuildStatus.BUILDING)
        )
        coEvery { repository.getAllProjects() } returns MutableStateFlow(projects)
        viewModel = ProjectsViewModel(repository)
        viewModel.setFilter(BuildStatus.SUCCESS)
        assertEquals(BuildStatus.SUCCESS, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun `filter null shows all projects`() = runTest {
        val projects = listOf(
            AppProject(id = "1", name = "A", description = "d", prompt = "p", appType = AppType.CALCULATOR, buildStatus = BuildStatus.SUCCESS),
            AppProject(id = "2", name = "B", description = "d", prompt = "p", appType = AppType.WEATHER, buildStatus = BuildStatus.BUILDING)
        )
        coEvery { repository.getAllProjects() } returns MutableStateFlow(projects)
        viewModel = ProjectsViewModel(repository)
        viewModel.setFilter(null)
        assertEquals(2, viewModel.uiState.value.filteredProjects.size)
    }
}
