package com.aiapkbuilder.app

import com.aiapkbuilder.app.data.service.build.BuildExecutor
import com.aiapkbuilder.app.data.service.build.BuildProviderFactory
import com.aiapkbuilder.app.data.service.build.LogAggregator
import com.aiapkbuilder.app.data.service.build.ArtifactManager
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.BuildStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildExecutorTest {

    private lateinit var buildExecutor: BuildExecutor
    private lateinit var providerFactory: BuildProviderFactory
    private lateinit var projectRepository: ProjectRepository
    private lateinit var logAggregator: LogAggregator
    private lateinit var artifactManager: ArtifactManager

    @Before
    fun setup() {
        providerFactory = mockk()
        projectRepository = mockk()
        logAggregator = mockk()
        artifactManager = mockk()

        buildExecutor = BuildExecutor(
            providerFactory, projectRepository, logAggregator, artifactManager
        )
    }

    @Test
    fun `executeBuild should create and monitor build job successfully`() = runTest {
        // Given
        val projectId = "test_project"
        val mockProvider = mockk<com.aiapkbuilder.app.data.service.build.IBuildProvider> {
            coEvery { provider } returns BuildProvider.GITHUB_ACTIONS
            coEvery { isHealthy() } returns true
            coEvery { startBuild(any(), any(), any()) } returns Result.success(
                BuildJob(
                    jobId = "test_job",
                    projectId = projectId,
                    provider = BuildProvider.GITHUB_ACTIONS,
                    status = BuildStatus.BUILDING
                )
            )
            coEvery { getBuildStatus(any()) } returns Result.success(
                BuildJob(
                    jobId = "test_job",
                    projectId = projectId,
                    provider = BuildProvider.GITHUB_ACTIONS,
                    status = BuildStatus.SUCCESS
                )
            )
        }

        coEvery { providerFactory.selectBestProvider() } returns mockProvider
        coEvery { projectRepository.createBuildJob(any()) } returns Unit
        coEvery { projectRepository.updateBuildJob(any()) } returns Unit
        coEvery { logAggregator.aggregateLogs(any(), any()) } returns Unit
        coEvery { artifactManager.registerArtifact(any(), any()) } returns Unit

        // When
        val result = buildExecutor.executeBuild(projectId)

        // Then
        assertTrue(result.isSuccess)
        val job = result.getOrNull()!!
        assertEquals(BuildStatus.SUCCESS, job.status)
        assertEquals(projectId, job.projectId)

        coVerify { projectRepository.createBuildJob(any()) }
        coVerify { projectRepository.updateBuildJob(any()) }
    }

    @Test
    fun `executeBuild should handle provider failure gracefully`() = runTest {
        // Given
        val projectId = "test_project"
        val mockProvider = mockk<com.aiapkbuilder.app.data.service.build.IBuildProvider> {
            coEvery { provider } returns BuildProvider.GITHUB_ACTIONS
            coEvery { isHealthy() } returns true
            coEvery { startBuild(any(), any(), any()) } returns Result.failure(
                Exception("Provider error")
            )
        }

        coEvery { providerFactory.selectBestProvider() } returns mockProvider
        coEvery { projectRepository.createBuildJob(any()) } returns Unit
        coEvery { projectRepository.updateBuildJob(any()) } returns Unit

        // When
        val result = buildExecutor.executeBuild(projectId)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Provider error", result.exceptionOrNull()?.message)

        coVerify { projectRepository.createBuildJob(any()) }
        coVerify { projectRepository.updateBuildJob(any()) }
    }

    @Test
    fun `cancelBuild should cancel running build successfully`() = runTest {
        // Given
        val jobId = "test_job"
        val mockProvider = mockk<com.aiapkbuilder.app.data.service.build.IBuildProvider> {
            coEvery { cancelBuild(jobId) } returns Result.success(true)
        }

        coEvery { projectRepository.getBuildJob(jobId) } returns BuildJob(
            jobId = jobId,
            projectId = "test_project",
            provider = BuildProvider.GITHUB_ACTIONS,
            status = BuildStatus.BUILDING
        )
        coEvery { providerFactory.getProvider(any()) } returns mockProvider
        coEvery { projectRepository.updateBuildJob(any()) } returns Unit

        // When
        val result = buildExecutor.cancelBuild(jobId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)

        coVerify { mockProvider.cancelBuild(jobId) }
        coVerify { projectRepository.updateBuildJob(any()) }
    }
}