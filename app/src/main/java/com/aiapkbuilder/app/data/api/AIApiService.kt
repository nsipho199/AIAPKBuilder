package com.aiapkbuilder.app.data.api

import com.aiapkbuilder.app.data.model.GeneratedProjectPlan
import retrofit2.Response
import retrofit2.http.*

// ─── OpenAI-Compatible API ────────────────────────────────────
data class OpenAIRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val max_tokens: Int = 4096,
    val temperature: Float = 0.7f,
    val response_format: ResponseFormat? = null
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

data class Choice(
    val message: ChatMessage,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

interface OpenAIApiService {
    @POST("chat/completions")
    suspend fun generateCompletion(
        @Header("Authorization") authorization: String,
        @Body request: OpenAIRequest
    ): Response<OpenAIResponse>
}

// ─── GitHub Actions API ───────────────────────────────────────
data class GitHubWorkflowDispatch(
    val ref: String = "main",
    val inputs: Map<String, String> = emptyMap()
)

data class GitHubRunStatus(
    val id: Long,
    val status: String,
    val conclusion: String?,
    val html_url: String
)

data class SimpleGitHubArtifactList(
    val artifacts: List<SimpleGitHubArtifact>
)

data class SimpleGitHubArtifact(
    val id: Long,
    val name: String,
    val archive_download_url: String,
    val size_in_bytes: Long
)

interface GitHubApiService {
    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun triggerWorkflow(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body body: GitHubWorkflowDispatch
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getRunStatus(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<GitHubRunStatus>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getRunArtifacts(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<SimpleGitHubArtifactList>
}
