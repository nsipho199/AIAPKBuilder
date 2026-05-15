package com.aiapkbuilder.app.data.api

import retrofit2.Response
import retrofit2.http.*

// ─── Codemagic API ────────────────────────────────────────────
data class CodemagicBuildRequest(
    val branch: String,
    val environment: Map<String, String> = emptyMap(),
    val customBuildLogsPath: String? = null
)

data class CodemagicBuild(
    val buildId: String,
    val status: String, // "queued", "building", "success", "failed", "canceled"
    val startedAt: String? = null,
    val finishedAt: String? = null,
    val duration: Long? = null
)

data class CodemagicBuildStatus(
    val build: CodemagicBuild,
    val buildNumber: Int,
    val commitHash: String?,
    val commitMessage: String?
)

data class CodemagicArtifacts(
    val artifacts: List<CodemagicArtifact>
)

data class CodemagicArtifact(
    val name: String,
    val type: String, // "apk", "aab", "ipa"
    val downloadUrl: String,
    val size: Long
)

interface CodemagicApiService {
    @POST("builds")
    suspend fun startBuild(
        @Header("Authorization") token: String,
        @Body request: CodemagicBuildRequest
    ): Response<CodemagicBuild>

    @GET("builds/{buildId}")
    suspend fun getBuildStatus(
        @Header("Authorization") token: String,
        @Path("buildId") buildId: String
    ): Response<CodemagicBuildStatus>

    @GET("builds/{buildId}/artifacts")
    suspend fun getArtifacts(
        @Header("Authorization") token: String,
        @Path("buildId") buildId: String
    ): Response<CodemagicArtifacts>

    @POST("builds/{buildId}/cancel")
    suspend fun cancelBuild(
        @Header("Authorization") token: String,
        @Path("buildId") buildId: String
    ): Response<Unit>
}
