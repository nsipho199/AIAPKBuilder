package com.aiapkbuilder.app.data.service.export

import android.content.Context
import com.aiapkbuilder.app.data.local.BackupRecordDao
import com.aiapkbuilder.app.data.local.BuildHistoryDao
import com.aiapkbuilder.app.data.model.BackupRecord
import com.aiapkbuilder.app.data.model.BackupStatus
import com.aiapkbuilder.app.data.model.StorageStats
import com.aiapkbuilder.app.data.service.build.ArtifactCache
import com.aiapkbuilder.app.util.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    private val backupRecordDao: BackupRecordDao,
    private val buildHistoryDao: BuildHistoryDao,
    private val artifactCache: ArtifactCache,
    @ApplicationContext private val context: Context
) {
    suspend fun getStorageStats(): Result<StorageStats> = withContext(Dispatchers.IO) {
        try {
            val artifactSize = artifactCache.getCacheSize()
            val cacheDir = File(context.cacheDir, "exports")
            val exportSize = if (cacheDir.exists()) cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L

            val backupCount = kotlinx.coroutines.flow.first(backupRecordDao.getTotalBackupSize(BackupStatus.COMPLETED)) ?: 0L

            val artifactCount = artifactCache.getCacheStats()["fileCount"] as? Int ?: 0
            val cacheCount = cacheDir.listFiles()?.size ?: 0

            val oldest = findOldestArtifactAge()

            val totalSize = artifactSize + exportSize + backupCount
            val recommendedCleanup = if (artifactSize > 500_000_000) artifactSize / 2 else 0L

            Result.success(StorageStats(
                totalArtifactSize = artifactSize,
                totalBackupSize = backupCount,
                totalCacheSize = exportSize,
                artifactCount = artifactCount,
                backupCount = 0,
                cacheCount = cacheCount,
                oldestArtifactAge = oldest,
                recommendedCleanup = recommendedCleanup
            ))
        } catch (e: Exception) {
            AppLogger.e("Failed to get storage stats", e)
            Result.failure(e)
        }
    }

    suspend fun cleanupOldArtifacts(maxAgeDays: Int = 30) = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
            val cacheDir = File(context.cacheDir, "exports")
            if (cacheDir.exists()) {
                cacheDir.walkTopDown().filter { it.isFile && it.lastModified() < cutoffTime }.forEach { file ->
                    file.delete()
                    AppLogger.i("Deleted old export: ${file.name}")
                }
            }

            AppLogger.i("Cleanup complete for artifacts older than $maxAgeDays days")
        } catch (e: Exception) {
            AppLogger.e("Failed to cleanup old artifacts", e)
        }
    }

    suspend fun createBackup(projectId: String, file: File): Result<BackupRecord> = withContext(Dispatchers.IO) {
        try {
            val backup = BackupRecord(
                backupId = UUID.randomUUID().toString(),
                projectId = projectId,
                fileName = file.name,
                fileSize = file.length(),
                status = BackupStatus.COMPLETED,
                storageProvider = "local",
                completedAt = System.currentTimeMillis()
            )
            backupRecordDao.insertBackup(backup)
            AppLogger.i("Backup created: ${backup.backupId}")
            Result.success(backup)
        } catch (e: Exception) {
            AppLogger.e("Failed to create backup", e)
            Result.failure(e)
        }
    }

    fun getBackupsForProject(projectId: String): Flow<List<BackupRecord>> {
        return backupRecordDao.getBackupsForProject(projectId)
    }

    suspend fun deleteBackup(backupId: String) = withContext(Dispatchers.IO) {
        backupRecordDao.deleteBackup(backupId)
    }

    suspend fun clearAllExports() = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        if (exportDir.exists()) {
            exportDir.deleteRecursively()
            AppLogger.i("Cleared all exports")
        }
    }

    suspend fun getTotalSize(): Long = withContext(Dispatchers.IO) {
        val stats = getStorageStats().getOrNull() ?: return@withContext 0L
        stats.totalArtifactSize + stats.totalBackupSize + stats.totalCacheSize
    }

    private suspend fun findOldestArtifactAge(): Long = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir)
        if (!cacheDir.exists()) return@withContext 0L
        val oldest = cacheDir.walkTopDown()
            .filter { it.isFile }
            .minOfOrNull { it.lastModified() } ?: System.currentTimeMillis()
        (System.currentTimeMillis() - oldest) / (24 * 60 * 60 * 1000L)
    }
}
