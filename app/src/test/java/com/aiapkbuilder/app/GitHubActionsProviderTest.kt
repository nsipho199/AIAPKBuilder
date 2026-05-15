package com.aiapkbuilder.app

import com.aiapkbuilder.app.data.api.GitHubActionsApiService
import com.aiapkbuilder.app.data.service.build.provider.GitHubActionsProvider
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.BuildStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class GitHubActionsProviderTest {

    private lateinit var provider: GitHubActionsProvider
    private lateinit var apiService: GitHubActionsApiService

    @Before
    fun setup() {
        apiService = mockk()
        provider = GitHubActionsProvider(apiService)
    }

    @Test
    fun `provider should return correct provider type`() {
        assertEquals(BuildProvider.GITHUB_ACTIONS, provider.provider)
    }

    @Test
    fun `isHealthy should return true when API is accessible`() = runTest {
        // Given
        coEvery { apiService.getWorkflows(any(), any(), any()) } returns Response.success(emptyList())

        // When
        val result = provider.isHealthy()

        // Then
        assertTrue(result)
    }

    @Test
    fun `startBuild should create build job successfully`() = runTest {
        // Given
        val projectId = "test_project"
        val sourcePath = "/path/to/source.zip"
        val config = mapOf("repoOwner" to "testowner", "repoName" to "testrepo")

        coEvery {
            apiService.dispatchWorkflow(any(), any(), any(), any(), any())
        } returns Response.success(Unit)

        // When
        val result = provider.startBuild(projectId, sourcePath, config)

        // Then
        assertTrue(result.isSuccess)
        val job = result.getOrNull()!!
        assertEquals(projectId, job.projectId)
        assertEquals(BuildProvider.GITHUB_ACTIONS, job.provider)
        assertEquals(BuildStatus.BUILDING, job.status)
    }

    @Test
    fun `getBuildStatus should return updated job status`() = runTest {
        // Given
        val jobId = "run_123"
        val mockRun = com.aiapkbuilder.app.data.api.GitHubWorkflowRun(
            id = 123L,
            status = "completed",
            conclusion = "success",
            createdAt = java.util.Date(),
            updatedAt = java.util.Date(),
            runNumber = 1
        )

        coEvery {
            apiService.getWorkflowRun(any(), any(), 123L, any())
        } returns Response.success(mockRun)

        // When
        val result = provider.getBuildStatus(jobId)

        // Then
        assertTrue(result.isSuccess)
        val job = result.getOrNull()!!
        assertEquals(BuildStatus.SUCCESS, job.status)
    }

    @Test
    fun `downloadArtifact should return artifact path on success`() = runTest {
        // Given
        val jobId = "run_123"
        val mockResponse = "mock apk content".toResponseBody()

        coEvery {
            apiService.getWorkflowRunArtifacts(any(), any(), 123L, any())
        } returns Response.success(
            com.aiapkbuilder.app.data.api.GitHubArtifactsResponse(
                artifacts = listOf(
                    com.aiapkbuilder.app.data.api.GitHubArtifact(
                        id = 456L,
                        name = "app-debug.apk",
                        sizeInBytes = 1024L,
                        url = "https://example.com",
                        downloadUrl = "https://download.example.com"
                    )
                )
            )
        )

        coEvery {
            apiService.downloadArtifact(any(), any(), 456L, any())
        } returns Response.success(mockResponse)

        // When
        val result = provider.downloadArtifact(jobId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.endsWith(".apk"))
    }

    @Test
    fun `cancelBuild should cancel workflow run successfully`() = runTest {
        // Given
        val jobId = "run_123"

        coEvery {
            apiService.cancelWorkflowRun(any(), any(), 123L, any())
        } returns Response.success(Unit)

        // When
        val result = provider.cancelBuild(jobId)

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    @Test
    fun `getEstimatedBuildTime should return reasonable time`() {
        val time = provider.getEstimatedBuildTime()
        assertTrue(time != null)
        assertTrue(time!! > 0)
    }
}