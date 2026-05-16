package com.aiapkbuilder.app.data.service.build

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrator for build execution across multiple providers.
 * Handles provider selection, fallback, state management, and real-time updates.
 */
@Singleton
class BuildExecutor @Inject constructor(
    private val providerFactory: BuildProviderFactory,
    private val projectRepository: ProjectRepository,
    private val logAggregator: LogAggregator,
    private val artifactManager: ArtifactManager
) {

    private val logger = AppLogger.getLogger("BuildExecutor")

    /**
     * Executes a build for a project using the best available provider.
     * @param projectId Project to build
     * @param preferredProvider Preferred build provider (optional)
     * @param onProgress Progress callback with percentage and message
     * @return BuildJob result
     */
    suspend fun executeBuild(
        projectId: String,
        preferredProvider: BuildProvider? = null,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<BuildJob> = withContext(Dispatchers.IO) {

        try {
            // Get project details
            val project = projectRepository.getProjectByIdOnce(projectId)
                ?: return@withContext Result.failure(Exception("Project not found"))

            // Determine build provider
            val provider = preferredProvider ?: providerFactory.selectBestProvider()
                ?: return@withContext Result.failure(Exception("No healthy build providers available"))

            onProgress(10, "Selected provider: ${provider.provider.displayName}")

            // Create build job
            val buildJob = BuildJob(
                jobId = generateJobId(),
                projectId = projectId,
                provider = provider.provider,
                status = BuildStatus.BUILDING,
                startedAt = System.currentTimeMillis()
            )

            projectRepository.createBuildJob(buildJob)
            onProgress(20, "Build job created")

            // Start the build
            val buildResult = provider.startBuild(
                projectId = projectId,
                sourceZipPath = project.sourceZipPath ?: "",
                config = getProviderConfig(provider.provider, projectId)
            )

            if (buildResult.isFailure) {
                val error = buildResult.exceptionOrNull() ?: Exception("Unknown build error")
                logger.e("Build failed to start", error)

                // Update job with failure
                val failedJob = buildJob.copy(
                    status = BuildStatus.FAILED,
                    completedAt = System.currentTimeMillis(),
                    errorMessage = error.message
                )
                projectRepository.updateBuildJob(failedJob)

                return@withContext Result.failure(error)
            }

            val startedJob = buildResult.getOrNull()!!
            projectRepository.updateBuildJob(startedJob)
            onProgress(30, "Build started on ${provider.provider.displayName}")

            // Monitor build progress
            return@withContext monitorBuildProgress(startedJob, provider, onProgress)

        } catch (e: Exception) {
            logger.e("Build execution failed", e)
            onProgress(0, "Build failed: ${e.message}")
            return@withContext Result.failure(e)
        }
    }

    /**
     * Monitors build progress and handles completion.
     */
    private suspend fun monitorBuildProgress(
        buildJob: BuildJob,
        provider: IBuildProvider,
        onProgress: (Int, String) -> Unit
    ): Result<BuildJob> {

        val jobId = buildJob.jobId

        // Start log aggregation
        val logJob = launch {
            logAggregator.aggregateLogs(jobId, provider.subscribeToLogs(jobId))
        }

        try {
            // Poll for status updates
            var lastProgress = 30
            var consecutiveErrors = 0
            val maxRetries = 10

            while (consecutiveErrors < maxRetries) {
                delay(5000) // Poll every 5 seconds

                val statusResult = provider.getBuildStatus(jobId)
                if (statusResult.isFailure) {
                    consecutiveErrors++
                    logger.w("Failed to get build status (attempt $consecutiveErrors)", statusResult.exceptionOrNull())
                    continue
                }

                val updatedJob = statusResult.getOrNull()!!
                projectRepository.updateBuildJob(updatedJob)

                // Update progress
                val progress = when (updatedJob.status) {
                    BuildStatus.BUILDING -> minOf(lastProgress + 5, 80)
                    BuildStatus.SUCCESS -> 90
                    BuildStatus.FAILED -> 0
                    else -> lastProgress
                }

                if (progress != lastProgress) {
                    onProgress(progress, getStatusMessage(updatedJob.status))
                    lastProgress = progress
                }

                // Check if build is complete
                if (updatedJob.status == BuildStatus.SUCCESS || updatedJob.status == BuildStatus.FAILED) {
                    logJob.cancel()

                    if (updatedJob.status == BuildStatus.SUCCESS) {
                        // Download artifact
                        onProgress(95, "Downloading artifact...")
                        val downloadResult = provider.downloadArtifact(jobId)
                        if (downloadResult.isSuccess) {
                            val artifactPath = downloadResult.getOrNull()!!
                            artifactManager.registerArtifact(updatedJob, artifactPath)
                            onProgress(100, "Build completed successfully!")
                        } else {
                            logger.w("Failed to download artifact", downloadResult.exceptionOrNull())
                        }
                    }

                    return Result.success(updatedJob)
                }

                consecutiveErrors = 0 // Reset error count on success
            }

            // Max retries exceeded
            val timeoutJob = buildJob.copy(
                status = BuildStatus.FAILED,
                completedAt = System.currentTimeMillis(),
                errorMessage = "Build monitoring timed out"
            )
            projectRepository.updateBuildJob(timeoutJob)
            logJob.cancel()

            return Result.failure(Exception("Build monitoring timed out"))

        } catch (e: Exception) {
            logger.e("Error monitoring build progress", e)
            logJob.cancel()
            return Result.failure(e)
        }
    }

    /**
     * Cancels a running build.
     */
    suspend fun cancelBuild(jobId: String): Result<Boolean> {
        return try {
            val job = projectRepository.getBuildJob(jobId)
                ?: return Result.failure(Exception("Build job not found"))

            val provider = providerFactory.getProvider(job.provider)
            val cancelResult = provider.cancelBuild(jobId)

            if (cancelResult.isSuccess) {
                val cancelledJob = job.copy(
                    status = BuildStatus.CANCELLED,
                    completedAt = System.currentTimeMillis()
                )
                projectRepository.updateBuildJob(cancelledJob)
            }

            cancelResult
        } catch (e: Exception) {
            logger.e("Failed to cancel build", e)
            Result.failure(e)
        }
    }

    /**
     * Retries a failed build with fallback provider.
     */
    suspend fun retryBuild(
        projectId: String,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Result<BuildJob> {
        // Mark previous builds as cancelled
        val previousJobs = projectRepository.getBuildJobsForProject(projectId)
            .firstOrNull()?.filter { it.status == BuildStatus.FAILED } ?: emptyList()

        previousJobs.forEach { job ->
            projectRepository.updateBuildJob(job.copy(status = BuildStatus.CANCELLED))
        }

        // Execute new build
        return executeBuild(projectId, onProgress = onProgress)
    }

    private fun generateJobId(): String = "build_${UUID.randomUUID()}"

    private fun getProviderConfig(provider: BuildProvider, projectId: String): Map<String, Any> {
        // TODO: Load from BuildConfig entity
        return emptyMap()
    }

    private fun getStatusMessage(status: BuildStatus): String {
        return when (status) {
            BuildStatus.PENDING -> "Build queued"
            BuildStatus.GENERATING -> "Preparing build"
            BuildStatus.BUILDING -> "Building APK"
            BuildStatus.SUCCESS -> "Build completed"
            BuildStatus.FAILED -> "Build failed"
            BuildStatus.CANCELLED -> "Build cancelled"
        }
    }
}