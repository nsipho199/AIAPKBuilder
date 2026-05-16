package com.aiapkbuilder.app.data.service.build

import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches build artifacts on disk for faster access and persistence.
 * Manages storage space and cleanup of cached files.
 */
@Singleton
class ArtifactCache @Inject constructor() {

    private val logger = AppLogger.getLogger("ArtifactCache")

    private val cacheDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".aiapkbuilder/cache/artifacts")
        dir.mkdirs()
        dir
    }

    private val maxCacheSizeBytes = 1024L * 1024L * 1024L * 5L // 5GB

    /**
     * Stores an artifact file in the cache.
     * @param sourceFile The source artifact file
     * @param jobId Build job ID for organization
     * @return Cached file path
     */
    suspend fun storeArtifact(sourceFile: File, jobId: String): String = withContext(Dispatchers.IO) {
        try {
            // Ensure cache directory exists
            cacheDir.mkdirs()

            // Create job-specific subdirectory
            val jobDir = File(cacheDir, jobId)
            jobDir.mkdirs()

            // Generate unique filename
            val fileName = "${sourceFile.nameWithoutExtension}_${System.currentTimeMillis()}.${sourceFile.extension}"
            val cachedFile = File(jobDir, fileName)

            // Copy file to cache
            Files.copy(sourceFile.toPath(), cachedFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            // Clean up old files if cache is too large
            cleanupIfNeeded()

            cachedFile.absolutePath

        } catch (e: Exception) {
            logger.e("Failed to store artifact in cache", e)
            throw e
        }
    }

    /**
     * Retrieves an artifact file from the cache.
     * @param cachedPath The cached file path
     * @return The cached file
     */
    fun getArtifact(cachedPath: String): File {
        return File(cachedPath)
    }

    /**
     * Checks if an artifact exists in the cache.
     * @param cachedPath The cached file path
     * @return true if the file exists
     */
    fun hasArtifact(cachedPath: String): Boolean {
        return File(cachedPath).exists()
    }

    /**
     * Deletes an artifact from the cache.
     * @param cachedPath The cached file path
     */
    suspend fun deleteArtifact(cachedPath: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(cachedPath)
            if (file.exists()) {
                file.delete()
                logger.i("Deleted cached artifact: $cachedPath")
            }
        } catch (e: Exception) {
            logger.w("Failed to delete cached artifact", e)
        }
    }

    /**
     * Gets the current cache size in bytes.
     * @return Total size of cached files
     */
    fun getCacheSize(): Long {
        return cacheDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * Gets cache statistics.
     * @return Map of cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        val files = cacheDir.walkTopDown().filter { it.isFile }.toList()
        val totalSize = files.sumOf { it.length() }
        val fileCount = files.size
        val jobDirs = cacheDir.listFiles { it.isDirectory }?.size ?: 0

        return mapOf(
            "totalSizeBytes" to totalSize,
            "fileCount" to fileCount,
            "jobCount" to jobDirs,
            "maxSizeBytes" to maxCacheSizeBytes,
            "usagePercent" to (totalSize.toDouble() / maxCacheSizeBytes * 100).toInt()
        )
    }

    /**
     * Clears the entire cache.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            logger.i("Cleared artifact cache")
        } catch (e: Exception) {
            logger.e("Failed to clear cache", e)
        }
    }

    /**
     * Cleans up old cache files based on LRU (Least Recently Used).
     * @param maxFiles Maximum number of files to keep per job
     */
    suspend fun cleanupOldFiles(maxFiles: Int = 5) = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles { it.isDirectory }?.forEach { jobDir ->
                val files = jobDir.listFiles { it.isFile }?.sortedBy { it.lastModified() } ?: emptyArray()

                if (files.size > maxFiles) {
                    val filesToDelete = files.take(files.size - maxFiles)
                    filesToDelete.forEach { file ->
                        file.delete()
                        logger.i("Cleaned up old cached file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.w("Failed to cleanup old files", e)
        }
    }

    /**
     * Optimizes cache by removing duplicates and organizing files.
     */
    suspend fun optimizeCache() = withContext(Dispatchers.IO) {
        try {
            cleanupOldFiles()
            cleanupIfNeeded()
            logger.i("Optimized artifact cache")
        } catch (e: Exception) {
            logger.e("Failed to optimize cache", e)
        }
    }

    private suspend fun cleanupIfNeeded() = withContext(Dispatchers.IO) {
        val currentSize = getCacheSize()
        if (currentSize > maxCacheSizeBytes) {
            logger.i("Cache size exceeded limit ($currentSize > $maxCacheSizeBytes), cleaning up...")

            // Delete oldest files until under limit
            val allFiles = cacheDir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.lastModified() }
                .toList()

            var deletedSize = 0L
            for (file in allFiles) {
                if (currentSize - deletedSize <= maxCacheSizeBytes * 0.8) break // Leave 20% free

                file.delete()
                deletedSize += file.length()
                logger.i("Deleted old cached file: ${file.name}")
            }
        }
    }
}