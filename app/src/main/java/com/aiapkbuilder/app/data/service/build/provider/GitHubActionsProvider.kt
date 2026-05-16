package com.aiapkbuilder.app.data.service.build.provider

import com.aiapkbuilder.app.data.api.GitHubActionsApiService
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.service.build.IBuildProvider
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GitHub Actions build provider implementation.
 * Uses GitHub Actions workflows to build Android projects.
 */
@Singleton
class GitHubActionsProvider @Inject constructor(
    private val apiService: GitHubActionsApiService
) : IBuildProvider {

    override val provider: BuildProvider = BuildProvider.GITHUB_ACTIONS

    override suspend fun isHealthy(): Boolean {
        return try {
            // Check if we can access GitHub API
            val response = apiService.getWorkflows("test", "test")
            response.isSuccessful
        } catch (e: Exception) {
            AppLogger.w("GitHub Actions health check failed", e)
            false
        }
    }

    override suspend fun getCostEstimate(): Double? {
        // GitHub Actions has free tier, but charges for minutes
        // Estimate based on typical build time
        return 0.008 * (getEstimatedBuildTime()?.div(60.0) ?: 10.0) // $0.008 per minute
    }

    override suspend fun startBuild(
        projectId: String,
        sourceZipPath: String,
        config: Map<String, Any>
    ): Result<BuildJob> {
        return try {
            val repoOwner = config["repoOwner"] as? String ?: "aiapkbuilder"
            val repoName = config["repoName"] as? String ?: "builds"
            val workflowId = config["workflowId"] as? String ?: "android-build.yml"

            // Upload source code to GitHub
            val sourceFile = File(sourceZipPath)
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file not found: $sourceZipPath"))
            }

            val uploadResult = uploadSourceCode(repoOwner, repoName, sourceFile, projectId)
            if (uploadResult.isFailure) {
                return uploadResult
            }

            // Trigger workflow
            val workflowInputs = mapOf(
                "project_id" to projectId,
                "source_path" to uploadResult.getOrNull()!!
            )

            val dispatchResult = apiService.dispatchWorkflow(
                owner = repoOwner,
                repo = repoName,
                workflowId = workflowId,
                inputs = workflowInputs
            )

            if (!dispatchResult.isSuccessful) {
                return Result.failure(Exception("Failed to dispatch workflow: ${dispatchResult.message()}"))
            }

            // Get the run ID from response or generate one
            val runId = extractRunId(dispatchResult) ?: "run_${System.currentTimeMillis()}"

            val buildJob = BuildJob(
                jobId = runId,
                projectId = projectId,
                provider = provider,
                status = BuildStatus.BUILDING,
                startedAt = System.currentTimeMillis()
            )

            Result.success(buildJob)

        } catch (e: Exception) {
            AppLogger.e("Failed to start GitHub Actions build", e)
            Result.failure(e)
        }
    }

    override suspend fun getBuildStatus(jobId: String): Result<BuildJob> {
        return try {
            // Parse jobId to get run details
            val parts = jobId.split("_")
            if (parts.size < 2) {
                return Result.failure(Exception("Invalid job ID format"))
            }

            val runId = parts[1].toLongOrNull()
                ?: return Result.failure(Exception("Invalid run ID"))

            val repoOwner = "aiapkbuilder" // TODO: Get from config
            val repoName = "builds"

            val runResponse = apiService.getWorkflowRun(repoOwner, repoName, runId)
            if (!runResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get workflow run: ${runResponse.message()}"))
            }

            val run = runResponse.body()
                ?: return Result.failure(Exception("Empty workflow run response"))

            val status = mapGitHubStatus(run.status, run.conclusion)

            Result.success(BuildJob(
                jobId = jobId,
                projectId = "", // TODO: Extract from run
                provider = provider,
                status = status,
                startedAt = run.createdAt.time,
                completedAt = run.updatedAt.time,
                logOutput = "", // Logs are streamed separately
                errorMessage = if (status == BuildStatus.FAILED) run.conclusion else null
            ))

        } catch (e: Exception) {
            AppLogger.e("Failed to get build status", e)
            Result.failure(e)
        }
    }

    override fun subscribeToLogs(jobId: String): Flow<String> = flow {
        try {
            // Parse jobId to get run details
            val parts = jobId.split("_")
            if (parts.size >= 2) {
                val runId = parts[1].toLongOrNull()
                if (runId != null) {
                    val repoOwner = "aiapkbuilder"
                    val repoName = "builds"

                    val logsResponse = apiService.getWorkflowRunLogs(repoOwner, repoName, runId)
                    if (logsResponse.isSuccessful) {
                        val logs = logsResponse.body()?.string() ?: ""
                        logs.lineSequence().forEach { line ->
                            emit(line)
                            kotlinx.coroutines.delay(100) // Simulate streaming
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to stream logs", e)
            emit("Error streaming logs: ${e.message}")
        }
    }

    override suspend fun cancelBuild(jobId: String): Result<Boolean> {
        return try {
            val parts = jobId.split("_")
            if (parts.size < 2) return Result.success(false)

            val runId = parts[1].toLongOrNull() ?: return Result.success(false)

            val repoOwner = "aiapkbuilder"
            val repoName = "builds"

            val cancelResult = apiService.cancelWorkflowRun(repoOwner, repoName, runId)
            Result.success(cancelResult.isSuccessful)

        } catch (e: Exception) {
            AppLogger.e("Failed to cancel build", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadArtifact(jobId: String): Result<String> {
        return try {
            val parts = jobId.split("_")
            if (parts.size < 2) return Result.failure(Exception("Invalid job ID"))

            val runId = parts[1].toLongOrNull()
                ?: return Result.failure(Exception("Invalid run ID"))

            val repoOwner = "aiapkbuilder"
            val repoName = "builds"

            val artifactsResponse = apiService.getWorkflowRunArtifacts(repoOwner, repoName, runId)
            if (!artifactsResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get artifacts: ${artifactsResponse.message()}"))
            }

            val artifacts = artifactsResponse.body()?.artifacts ?: emptyList()
            val apkArtifact = artifacts.find { it.name.contains("apk", ignoreCase = true) }
                ?: artifacts.firstOrNull()

            if (apkArtifact == null) {
                return Result.failure(Exception("No APK artifact found"))
            }

            val downloadResponse = apiService.downloadArtifact(
                repoOwner, repoName, apkArtifact.id
            )

            if (!downloadResponse.isSuccessful) {
                return Result.failure(Exception("Failed to download artifact: ${downloadResponse.message()}"))
            }

            // Save to temp file
            val tempFile = File.createTempFile("artifact_", ".apk")
            downloadResponse.body()?.byteStream()?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(tempFile.absolutePath)

        } catch (e: Exception) {
            AppLogger.e("Failed to download artifact", e)
            Result.failure(e)
        }
    }

    override fun getEstimatedBuildTime(): Int? = 600 // 10 minutes

    private suspend fun uploadSourceCode(
        repoOwner: String,
        repoName: String,
        sourceFile: File,
        projectId: String
    ): Result<String> {
        return try {
            // Create a release or upload as asset
            val releaseResponse = apiService.createRelease(
                owner = repoOwner,
                repo = repoName,
                tagName = "build_$projectId",
                name = "Build $projectId",
                body = "Automated build for project $projectId"
            )

            if (!releaseResponse.isSuccessful) {
                return Result.failure(Exception("Failed to create release: ${releaseResponse.message()}"))
            }

            val release = releaseResponse.body()
                ?: return Result.failure(Exception("Empty release response"))

            // Upload source zip as release asset
            val requestBody = sourceFile.asRequestBody("application/zip".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("file", sourceFile.name, requestBody)

            val uploadResponse = apiService.uploadReleaseAsset(
                owner = repoOwner,
                repo = repoName,
                releaseId = release.id,
                assetName = "${projectId}_source.zip",
                file = multipartBody
            )

            if (!uploadResponse.isSuccessful) {
                return Result.failure(Exception("Failed to upload source: ${uploadResponse.message()}"))
            }

            Result.success("${repoOwner}/${repoName}/releases/download/${release.tagName}/${projectId}_source.zip")

        } catch (e: Exception) {
            AppLogger.e("Failed to upload source code", e)
            Result.failure(e)
        }
    }

    private fun extractRunId(dispatchResult: retrofit2.Response<*>): String? {
        // GitHub API doesn't return run ID immediately, so we generate one
        return null // Will be handled by polling
    }

    private fun mapGitHubStatus(status: String?, conclusion: String?): BuildStatus {
        return when {
            status == "completed" && conclusion == "success" -> BuildStatus.SUCCESS
            status == "completed" && conclusion == "failure" -> BuildStatus.FAILED
            status == "in_progress" || status == "queued" -> BuildStatus.BUILDING
            else -> BuildStatus.PENDING
        }
    }
}