package com.aiapkbuilder.app.data.service.build

import com.aiapkbuilder.app.data.model.BuildArtifact
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages build artifacts (APKs, AABs, source code).
 * Handles caching, storage, and download services.
 */
@Singleton
class ArtifactManager @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val artifactCache: ArtifactCache
) {

    /**
     * Registers a new artifact from a build.
     * @param buildJob The completed build job
     * @param artifactPath Local path to the artifact file
     */
    suspend fun registerArtifact(buildJob: BuildJob, artifactPath: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(artifactPath)
            if (!file.exists()) {
                AppLogger.w("Artifact file not found: $artifactPath")
                return@withContext
            }

            val fileSize = file.length()
            val sha256 = calculateSha256(file)
            val artifactType = determineArtifactType(file.name)

            // Move file to cache
            val cachedPath = artifactCache.storeArtifact(file, buildJob.jobId)

            val artifact = BuildArtifact(
                id = generateArtifactId(),
                buildJobId = buildJob.jobId,
                projectId = buildJob.projectId,
                artifactType = artifactType,
                localPath = cachedPath,
                fileName = file.name,
                fileSizeBytes = fileSize,
                sha256Hash = sha256,
                createdAt = System.currentTimeMillis()
            )

            projectRepository.saveArtifact(artifact)
            AppLogger.i("Registered artifact: ${artifact.fileName}")

        } catch (e: Exception) {
            AppLogger.e("Failed to register artifact", e)
        }
    }

    /**
     * Gets download URL for an artifact.
     * @param artifactId Artifact ID
     * @return Download URL or null if not available
     */
    suspend fun getDownloadUrl(artifactId: String): String? = withContext(Dispatchers.IO) {
        try {
            val artifact = runBlocking {
                projectRepository.getArtifactsForBuild("").first().find { it.id == artifactId }
            } ?: return@withContext null

            // For now, return local file path
            // In production, this would generate a secure download URL
            artifact.localPath

        } catch (e: Exception) {
            AppLogger.e("Failed to get download URL", e)
            null
        }
    }

    /**
     * Downloads an artifact to a temporary location.
     * @param artifactId Artifact ID
     * @return Local file path to downloaded artifact
     */
    suspend fun downloadArtifact(artifactId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val artifact = runBlocking {
                projectRepository.getArtifactsForBuild("").first().find { it.id == artifactId }
            } ?: return@withContext Result.failure(Exception("Artifact not found"))

            val cachedFile = artifactCache.getArtifact(artifact.localPath ?: "")
            if (cachedFile.exists()) {
                // Copy to temp location for download
                val tempFile = File.createTempFile("download_", "_${artifact.fileName}")
                cachedFile.copyTo(tempFile, overwrite = true)
                Result.success(tempFile.absolutePath)
            } else {
                Result.failure(Exception("Artifact file not found in cache"))
            }

        } catch (e: Exception) {
            AppLogger.e("Failed to download artifact", e)
            Result.failure(e)
        }
    }

    /**
     * Cleans up old artifacts to free disk space.
     * @param maxAgeDays Maximum age in days for artifacts to keep
     */
    suspend fun cleanupOldArtifacts(maxAgeDays: Int = 30) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)

            val oldArtifacts = runBlocking {
                projectRepository.getArtifactsForProject("").first().filter { it.createdAt < cutoffTime }
            }

            oldArtifacts.forEach { artifact ->
                try {
                    artifact.localPath?.let { path ->
                        artifactCache.deleteArtifact(path)
                    }
                    projectRepository.deleteArtifact(artifact.id)
                    AppLogger.i("Cleaned up old artifact: ${artifact.fileName}")
                } catch (e: Exception) {
                    AppLogger.w("Failed to cleanup artifact ${artifact.id}", e)
                }
            }

        } catch (e: Exception) {
            AppLogger.e("Failed to cleanup old artifacts", e)
        }
    }

    /**
     * Gets artifact statistics for a project.
     * @param projectId Project ID
     * @return Map of statistics
     */
    suspend fun getArtifactStats(projectId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val artifacts = projectRepository.getArtifactsForProject(projectId).first()
            val totalSize = artifacts.sumOf { it.fileSizeBytes }

            mapOf(
                "totalArtifacts" to artifacts.size,
                "totalSizeBytes" to totalSize,
                "artifactTypes" to artifacts.groupBy { it.artifactType }.mapValues { it.value.size }
            )
        } catch (e: Exception) {
            AppLogger.e("Failed to get artifact stats", e)
            emptyMap()
        }
    }

    /**
     * Validates artifact integrity using SHA256 hash.
     * @param artifactId Artifact ID
     * @return true if artifact is valid
     */
    suspend fun validateArtifact(artifactId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val artifact = runBlocking {
                projectRepository.getArtifactsForBuild("").first().find { it.id == artifactId }
            } ?: return@withContext false

            val file = artifactCache.getArtifact(artifact.localPath ?: "")
            if (!file.exists()) return@withContext false

            val actualSha256 = calculateSha256(file)
            actualSha256 == artifact.sha256Hash

        } catch (e: Exception) {
            AppLogger.e("Failed to validate artifact", e)
            false
        }
    }

    private fun generateArtifactId(): String = "artifact_${System.currentTimeMillis()}_${(0..999).random()}"

    private fun determineArtifactType(fileName: String): String {
        return when {
            fileName.endsWith(".apk", ignoreCase = true) -> "apk"
            fileName.endsWith(".aab", ignoreCase = true) -> "aab"
            fileName.endsWith(".zip", ignoreCase = true) -> "source"
            else -> "unknown"
        }
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}