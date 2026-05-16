package com.aiapkbuilder.app.data.service.export

import com.aiapkbuilder.app.data.local.BuildHistoryDao
import com.aiapkbuilder.app.data.model.BuildHistory
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildHistoryManager @Inject constructor(
    private val buildHistoryDao: BuildHistoryDao
) {
    suspend fun recordBuild(
        projectId: String,
        buildJobId: String,
        versionName: String,
        versionCode: Int,
        sizeBytes: Long,
        buildDurationMs: Long,
        provider: String,
        success: Boolean,
        tags: List<String> = emptyList(),
        notes: String = ""
    ) = withContext(Dispatchers.IO) {
        try {
            val history = BuildHistory(
                historyId = UUID.randomUUID().toString(),
                projectId = projectId,
                buildJobId = buildJobId,
                versionName = versionName,
                versionCode = versionCode,
                builtAt = System.currentTimeMillis(),
                sizeBytes = sizeBytes,
                buildDurationMs = buildDurationMs,
                provider = provider,
                success = success,
                tags = tags,
                notes = notes
            )
            buildHistoryDao.insertHistory(history)
            AppLogger.i("Build history recorded: ${history.historyId}")
        } catch (e: Exception) {
            AppLogger.e("Failed to record build history", e)
        }
    }

    fun getHistoryForProject(projectId: String): Flow<List<BuildHistory>> {
        return buildHistoryDao.getHistoryForProject(projectId)
    }

    fun getRecentHistory(projectId: String, since: Long): Flow<List<BuildHistory>> {
        return buildHistoryDao.getRecentHistory(projectId, since)
    }

    suspend fun getSuccessfulBuilds(projectId: String, limit: Int = 10): List<BuildHistory> {
        return buildHistoryDao.getSuccessfulBuilds(projectId, limit)
    }

    suspend fun deleteHistory(historyId: String) = withContext(Dispatchers.IO) {
        buildHistoryDao.deleteHistory(historyId)
    }

    fun getHistoryCount(projectId: String): Flow<Int> {
        return buildHistoryDao.getHistoryCount(projectId)
    }

    suspend fun updateHistory(history: BuildHistory) = withContext(Dispatchers.IO) {
        buildHistoryDao.updateHistory(history)
    }

    suspend fun clearProjectHistory(projectId: String) = withContext(Dispatchers.IO) {
        buildHistoryDao.getHistoryForProject(projectId).let { flow ->
            kotlinx.coroutines.flow.first(flow).forEach { history ->
                buildHistoryDao.deleteHistory(history.historyId)
            }
        }
    }

    suspend fun getLatestSuccessfulVersion(projectId: String): BuildHistory? {
        return buildHistoryDao.getSuccessfulBuilds(projectId, 1).firstOrNull()
    }

    suspend fun getBuildHistoryStats(projectId: String): Map<String, Any> = withContext(Dispatchers.IO) {
        try {
            val history = kotlinx.coroutines.flow.first(buildHistoryDao.getHistoryForProject(projectId))
            val totalBuilds = history.size
            val successfulBuilds = history.count { it.success }
            val failedBuilds = totalBuilds - successfulBuilds
            val totalSize = history.sumOf { it.sizeBytes }
            val avgDuration = if (totalBuilds > 0) history.sumOf { it.buildDurationMs } / totalBuilds else 0L

            mapOf(
                "totalBuilds" to totalBuilds,
                "successfulBuilds" to successfulBuilds,
                "failedBuilds" to failedBuilds,
                "successRate" to if (totalBuilds > 0) (successfulBuilds.toDouble() / totalBuilds * 100).toInt() else 0,
                "totalSizeBytes" to totalSize,
                "averageDurationMs" to avgDuration
            )
        } catch (e: Exception) {
            AppLogger.e("Failed to get build history stats", e)
            emptyMap()
        }
    }
}
