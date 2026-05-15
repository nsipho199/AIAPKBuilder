package com.aiapkbuilder.app.viewmodel

import android.content.Context
import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.service.build.BuildExecutor
import com.aiapkbuilder.app.data.service.build.LogAggregator
import com.aiapkbuilder.app.data.service.export.BuildHistoryManager
import com.aiapkbuilder.app.data.service.export.DownloadManager
import com.aiapkbuilder.app.data.service.export.ProjectExporter
import com.aiapkbuilder.app.data.service.export.ShareService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProjectDetailViewModelTest {

    private lateinit var viewModel: ProjectDetailViewModel
    private lateinit var repository: ProjectRepository
    private lateinit var buildExecutor: BuildExecutor
    private lateinit var logAggregator: LogAggregator
    private lateinit var downloadManager: DownloadManager
    private lateinit var projectExporter: ProjectExporter
    private lateinit var shareService: ShareService
    private lateinit var buildHistoryManager: BuildHistoryManager
    private lateinit var context: Context

    @Before
    fun setup() {
        repository = mockk()
        buildExecutor = mockk()
        logAggregator = mockk()
        downloadManager = mockk()
        projectExporter = mockk()
        shareService = mockk()
        buildHistoryManager = mockk()
        context = mockk()
    }

    @Test
    fun `initial ui state has defaults`() {
        coEvery { repository.getProject(any()) } returns MutableStateFlow(null)
        coEvery { repository.getBuildJobsForProject(any()) } returns MutableStateFlow(emptyList())
        coEvery { repository.getLatestBuildJob(any()) } returns MutableStateFlow(null)
        coEvery { buildHistoryManager.getHistoryCount(any()) } returns MutableStateFlow(0)

        viewModel = ProjectDetailViewModel(repository, buildExecutor, logAggregator, downloadManager, projectExporter, shareService, buildHistoryManager, context)
        viewModel.loadProject("test_id")

        val state = viewModel.uiState.value
        assertNull(state.project)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadProject fetches project details`() = runTest {
        val project = AppProject(id = "1", name = "Weather App", description = "desc", prompt = "Build weather", appType = AppType.WEATHER, buildStatus = BuildStatus.SUCCESS)
        coEvery { repository.getProject("1") } returns MutableStateFlow(project)
        coEvery { repository.getBuildJobsForProject("1") } returns MutableStateFlow(emptyList())
        coEvery { repository.getLatestBuildJob("1") } returns MutableStateFlow(null)
        coEvery { buildHistoryManager.getHistoryCount("1") } returns MutableStateFlow(0)

        viewModel = ProjectDetailViewModel(repository, buildExecutor, logAggregator, downloadManager, projectExporter, shareService, buildHistoryManager, context)
        viewModel.loadProject("1")

        assertEquals("Weather App", viewModel.uiState.value.project?.name)
    }
}
