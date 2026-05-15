package com.aiapkbuilder.app.data.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// ─── GitHub Actions API Models ────────────────────────────────
data class GitHubWorkflowDispatchRequest(
    val ref: String = "main",
    val inputs: Map<String, String> = emptyMap()
)

data class GitHubWorkflowRun(
    val id: Long,
    val status: String, // "queued", "in_progress", "completed"
    val conclusion: String?, // "success", "failure", "cancelled", etc.
    val createdAt: java.util.Date,
    val updatedAt: java.util.Date,
    val runNumber: Int
)

data class GitHubWorkflowRunsResponse(
    val workflowRuns: List<GitHubWorkflowRun>
)

data class GitHubArtifact(
    val id: Long,
    val name: String,
    val sizeInBytes: Long,
    val url: String,
    val downloadUrl: String,
    val createdAt: String
)

data class GitHubArtifactsResponse(
    val artifacts: List<GitHubArtifact>
)

data class GitHubRelease(
    val id: Long,
    val tagName: String,
    val name: String,
    val body: String,
    val createdAt: String,
    val htmlUrl: String
)

data class GitHubReleaseRequest(
    val tagName: String,
    val name: String,
    val body: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false
)

interface GitHubActionsApiService {

    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun getWorkflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") token: String? = null
    ): Response<GitHubWorkflowRunsResponse>

    @POST("repos/{owner}/{repo}/actions/workflows/{workflowId}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflowId") workflowId: String,
        @Body request: GitHubWorkflowDispatchRequest,
        @Header("Authorization") token: String
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}")
    suspend fun getWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long,
        @Header("Authorization") token: String? = null
    ): Response<GitHubWorkflowRun>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/logs")
    suspend fun getWorkflowRunLogs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long,
        @Header("Authorization") token: String? = null
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun getWorkflowRunArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long,
        @Header("Authorization") token: String? = null
    ): Response<GitHubArtifactsResponse>

    @GET("repos/{owner}/{repo}/actions/artifacts/{artifactId}/zip")
    suspend fun downloadArtifact(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifactId") artifactId: Long,
        @Header("Authorization") token: String? = null
    ): Response<ResponseBody>

    @POST("repos/{owner}/{repo}/actions/runs/{runId}/cancel")
    suspend fun cancelWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long,
        @Header("Authorization") token: String
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/releases")
    suspend fun createRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: GitHubReleaseRequest,
        @Header("Authorization") token: String
    ): Response<GitHubRelease>

    @POST("repos/{owner}/{repo}/releases/{releaseId}/assets")
    suspend fun uploadReleaseAsset(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("releaseId") releaseId: Long,
        @Query("name") assetName: String,
        @Body file: MultipartBody.Part,
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String = "application/octet-stream"
    ): Response<Unit>
}