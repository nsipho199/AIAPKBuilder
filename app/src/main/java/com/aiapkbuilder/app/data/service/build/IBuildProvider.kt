package com.aiapkbuilder.app.data.service.build

import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildProvider
import kotlinx.coroutines.flow.Flow

/**
 * Interface for build providers that can compile Android projects into APKs/AABs.
 * Supports multiple cloud providers with unified API.
 */
interface IBuildProvider {

    val provider: BuildProvider

    /**
     * Checks if this provider is available and healthy.
     * @return true if provider can accept builds
     */
    suspend fun isHealthy(): Boolean

    /**
     * Gets the current cost estimate for a build.
     * @return cost in USD, or null if not applicable
     */
    suspend fun getCostEstimate(): Double?

    /**
     * Starts a build for the given project.
     * @param projectId Project to build
     * @param sourceZipPath Path to zipped source code
     * @param config Provider-specific configuration
     * @return BuildJob with provider-specific job ID
     */
    suspend fun startBuild(
        projectId: String,
        sourceZipPath: String,
        config: Map<String, Any>
    ): Result<BuildJob>

    /**
     * Gets the current status of a build job.
     * @param jobId Provider-specific job identifier
     * @return Updated BuildJob with current status
     */
    suspend fun getBuildStatus(jobId: String): Result<BuildJob>

    /**
     * Subscribes to real-time build logs.
     * @param jobId Provider-specific job identifier
     * @return Flow of log lines as they arrive
     */
    fun subscribeToLogs(jobId: String): Flow<String>

    /**
     * Cancels a running build.
     * @param jobId Provider-specific job identifier
     * @return true if cancellation succeeded
     */
    suspend fun cancelBuild(jobId: String): Result<Boolean>

    /**
     * Downloads the build artifact (APK/AAB).
     * @param jobId Provider-specific job identifier
     * @return Local file path to downloaded artifact
     */
    suspend fun downloadArtifact(jobId: String): Result<String>

    /**
     * Gets estimated build time in seconds.
     * @return Estimated duration, or null if unknown
     */
    fun getEstimatedBuildTime(): Int?
}