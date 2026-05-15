package com.aiapkbuilder.app.data.service

import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildStatus
import kotlinx.coroutines.flow.Flow

/**
 * Interface for build provider service.
 * Handles compilation and APK generation across different providers.
 */
interface BuildProviderService {
    /**
     * Initiates a build for a project.
     * @param projectId ID of the project to build
     * @param config Build configuration JSON
     * @return BuildJob result
     */
    suspend fun startBuild(projectId: String, config: String): Result<BuildJob>

    /**
     * Gets the status of a running build.
     * @param jobId Build job ID
     * @return Current build job with updated status
     */
    suspend fun getBuildStatus(jobId: String): Result<BuildJob>

    /**
     * Subscribes to real-time build logs.
     * @param jobId Build job ID
     * @return Flow of log lines
     */
    fun subscribeToLogs(jobId: String): Flow<String>

    /**
     * Cancels a running build.
     * @param jobId Build job ID
     * @return True if cancellation was successful
     */
    suspend fun cancelBuild(jobId: String): Result<Boolean>

    /**
     * Downloads the built APK artifact.
     * @param jobId Build job ID
     * @return Local file path to downloaded APK
     */
    suspend fun downloadArtifact(jobId: String): Result<String>

    /**
     * Gets estimated build time for a project.
     * @param projectId Project ID
     * @param appType Type of app
     * @return Estimated seconds
     */
    suspend fun getEstimatedBuildTime(projectId: String, appType: String): Result<Int>

    /**
     * Validates build configuration.
     * @param config Build config JSON
     * @return Error message or null if valid
     */
    suspend fun validateBuildConfig(config: String): Result<String?>
}
