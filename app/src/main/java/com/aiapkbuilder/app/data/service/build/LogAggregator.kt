package com.aiapkbuilder.app.data.service.build

import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates and manages build logs from multiple providers.
 * Handles real-time streaming, persistence, and log retrieval.
 */
@Singleton
class LogAggregator @Inject constructor(
    private val projectRepository: ProjectRepository
) {

    private val logger = AppLogger.getLogger("LogAggregator")

    // In-memory log storage for active builds
    private val activeLogs = ConcurrentHashMap<String, MutableList<String>>()

    // Active log collectors
    private val activeCollectors = ConcurrentHashMap<String, Job>()

    /**
     * Aggregates logs from a provider's log stream.
     * @param jobId Build job ID
     * @param logStream Flow of log lines from the provider
     */
    suspend fun aggregateLogs(jobId: String, logStream: Flow<String>) {
        val logLines = mutableListOf<String>()
        activeLogs[jobId] = logLines

        val collectorJob = coroutineScope {
            launch {
                try {
                    logStream.collect { line ->
                        logLines.add(line)
                        persistLogLine(jobId, line)
                    }
                } catch (e: Exception) {
                    logger.e("Error aggregating logs for job $jobId", e)
                } finally {
                    // Persist final log state
                    persistCompleteLog(jobId, logLines)
                    activeLogs.remove(jobId)
                    activeCollectors.remove(jobId)
                }
            }
        }

        activeCollectors[jobId] = collectorJob
        collectorJob.join()
    }

    /**
     * Gets the current logs for a build job.
     * @param jobId Build job ID
     * @return List of log lines
     */
    fun getLogs(jobId: String): List<String> {
        return activeLogs[jobId] ?: getPersistedLogs(jobId)
    }

    /**
     * Gets a flow of log updates for a build job.
     * @param jobId Build job ID
     * @return Flow of log lines as they arrive
     */
    fun getLogFlow(jobId: String): Flow<String> = flow {
        val existingLogs = getLogs(jobId)
        existingLogs.forEach { emit(it) }

        // If build is still active, stream new logs
        activeLogs[jobId]?.let { logLines ->
            val currentSize = logLines.size
            while (activeLogs.containsKey(jobId)) {
                delay(1000) // Poll for new logs
                val newLines = logLines.drop(currentSize)
                newLines.forEach { emit(it) }
            }
        }
    }

    /**
     * Clears logs for a completed build job.
     * @param jobId Build job ID
     */
    fun clearLogs(jobId: String) {
        activeLogs.remove(jobId)
        activeCollectors[jobId]?.cancel()
        activeCollectors.remove(jobId)
    }

    /**
     * Gets the last N lines of logs.
     * @param jobId Build job ID
     * @param lines Number of lines to retrieve
     * @return Last N log lines
     */
    fun getLastLogs(jobId: String, lines: Int = 50): List<String> {
        val allLogs = getLogs(jobId)
        return if (allLogs.size <= lines) allLogs else allLogs.takeLast(lines)
    }

    /**
     * Searches logs for a pattern.
     * @param jobId Build job ID
     * @param pattern Search pattern (regex supported)
     * @return Matching log lines
     */
    fun searchLogs(jobId: String, pattern: String): List<String> {
        val allLogs = getLogs(jobId)
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
        return allLogs.filter { regex.containsMatchIn(it) }
    }

    /**
     * Gets build statistics from logs.
     * @param jobId Build job ID
     * @return Map of statistics
     */
    fun getLogStats(jobId: String): Map<String, Any> {
        val logs = getLogs(jobId)
        return mapOf(
            "totalLines" to logs.size,
            "errors" to logs.count { it.contains("error", ignoreCase = true) },
            "warnings" to logs.count { it.contains("warning", ignoreCase = true) },
            "buildSteps" to extractBuildSteps(logs)
        )
    }

    private fun extractBuildSteps(logs: List<String>): List<String> {
        val steps = mutableListOf<String>()
        val stepPatterns = listOf(
            "gradle" to "Gradle",
            "compile" to "Compiling",
            "dex" to "Dexing",
            "package" to "Packaging",
            "sign" to "Signing"
        )

        logs.forEach { line ->
            stepPatterns.forEach { (pattern, stepName) ->
                if (line.contains(pattern, ignoreCase = true) && stepName !in steps) {
                    steps.add(stepName)
                }
            }
        }

        return steps
    }

    private suspend fun persistLogLine(jobId: String, line: String) {
        try {
            // Update the build job with incremental log
            val job = projectRepository.getBuildJob(jobId)
            if (job != null) {
                val updatedLog = job.logOutput + line + "\n"
                projectRepository.updateBuildJob(job.copy(logOutput = updatedLog))
            }
        } catch (e: Exception) {
            logger.w("Failed to persist log line", e)
        }
    }

    private suspend fun persistCompleteLog(jobId: String, logLines: List<String>) {
        try {
            val completeLog = logLines.joinToString("\n")
            val job = projectRepository.getBuildJob(jobId)
            if (job != null) {
                projectRepository.updateBuildJob(job.copy(logOutput = completeLog))
            }
        } catch (e: Exception) {
            logger.w("Failed to persist complete log", e)
        }
    }

    private fun getPersistedLogs(jobId: String): List<String> {
        return try {
            val job = runBlocking { projectRepository.getBuildJob(jobId) }
            job?.logOutput?.lines() ?: emptyList()
        } catch (e: Exception) {
            logger.w("Failed to get persisted logs", e)
            emptyList()
        }
    }
}