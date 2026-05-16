package com.aiapkbuilder.app.data.service.build.provider

import com.aiapkbuilder.app.data.service.build.docker.DockerService
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.service.build.IBuildProvider
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Docker build provider implementation.
 * Uses local Docker containers for Android builds.
 */
@Singleton
class DockerProvider @Inject constructor(
    private val dockerService: DockerService
) : IBuildProvider {

    override val provider: BuildProvider = BuildProvider.DOCKER

    override suspend fun isHealthy(): Boolean {
        return dockerService.isDockerAvailable()
    }

    override suspend fun getCostEstimate(): Double? {
        // Local builds are free
        return 0.0
    }

    override suspend fun startBuild(
        projectId: String,
        sourceZipPath: String,
        config: Map<String, Any>
    ): Result<BuildJob> {
        return try {
            val sourceFile = File(sourceZipPath)
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source file not found: $sourceZipPath"))
            }

            val containerId = dockerService.createBuildContainer(projectId, sourceFile)

            val buildJob = BuildJob(
                jobId = containerId,
                projectId = projectId,
                provider = provider,
                status = BuildStatus.BUILDING,
                startedAt = System.currentTimeMillis()
            )

            // Start the build asynchronously
            dockerService.startBuild(containerId)

            Result.success(buildJob)

        } catch (e: Exception) {
            AppLogger.e("Failed to start Docker build", e)
            Result.failure(e)
        }
    }

    override suspend fun getBuildStatus(jobId: String): Result<BuildJob> {
        return try {
            val status = dockerService.getContainerStatus(jobId)
            val exitCode = dockerService.getContainerExitCode(jobId)

            val buildStatus = when {
                status.contains("running") -> BuildStatus.BUILDING
                exitCode == 0 -> BuildStatus.SUCCESS
                exitCode != null -> BuildStatus.FAILED
                else -> BuildStatus.PENDING
            }

            Result.success(BuildJob(
                jobId = jobId,
                projectId = "", // TODO: Extract from container
                provider = provider,
                status = buildStatus,
                startedAt = System.currentTimeMillis(), // TODO: Get actual start time
                completedAt = if (buildStatus == BuildStatus.SUCCESS || buildStatus == BuildStatus.FAILED)
                    System.currentTimeMillis() else null,
                logOutput = "", // Logs are streamed separately
                errorMessage = if (exitCode != 0 && exitCode != null) "Build failed with exit code $exitCode" else null
            ))

        } catch (e: Exception) {
            AppLogger.e("Failed to get build status", e)
            Result.failure(e)
        }
    }

    override fun subscribeToLogs(jobId: String): Flow<String> = flow {
        try {
            dockerService.getContainerLogs(jobId).collect { logLine ->
                emit(logLine)
            }
        } catch (e: Exception) {
            AppLogger.e("Failed to stream logs", e)
            emit("Error streaming logs: ${e.message}")
        }
    }

    override suspend fun cancelBuild(jobId: String): Result<Boolean> {
        return try {
            dockerService.stopContainer(jobId)
            Result.success(true)
        } catch (e: Exception) {
            AppLogger.e("Failed to cancel build", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadArtifact(jobId: String): Result<String> {
        return try {
            val artifactPath = dockerService.extractArtifact(jobId)
            Result.success(artifactPath)
        } catch (e: Exception) {
            AppLogger.e("Failed to download artifact", e)
            Result.failure(e)
        }
    }

    override fun getEstimatedBuildTime(): Int? = 600 // 10 minutes
}