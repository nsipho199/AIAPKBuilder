package com.aiapkbuilder.app.util

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Logging utility for the application
 */
object AppLogger {
    private const val TAG = "AIAPKBuilder"
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun initialize(logFilePath: String) {
        logFile = File(logFilePath)
        logFile?.parentFile?.mkdirs()
    }

    fun d(message: String, tag: String = TAG) {
        val formattedMessage = formatMessage(message)
        Log.d(tag, formattedMessage)
        writeToFile("DEBUG", tag, formattedMessage)
    }

    fun i(message: String, tag: String = TAG) {
        val formattedMessage = formatMessage(message)
        Log.i(tag, formattedMessage)
        writeToFile("INFO", tag, formattedMessage)
    }

    fun w(message: String, throwable: Throwable? = null, tag: String = TAG) {
        val formattedMessage = formatMessage(message)
        if (throwable != null) {
            Log.w(tag, formattedMessage, throwable)
        } else {
            Log.w(tag, formattedMessage)
        }
        writeToFile("WARN", tag, formattedMessage, throwable)
    }

    fun e(message: String, throwable: Throwable? = null, tag: String = TAG) {
        val formattedMessage = formatMessage(message)
        if (throwable != null) {
            Log.e(tag, formattedMessage, throwable)
        } else {
            Log.e(tag, formattedMessage)
        }
        writeToFile("ERROR", tag, formattedMessage, throwable)
    }

    private fun formatMessage(message: String): String =
        "[${dateFormat.format(Date())}] $message"

    private fun writeToFile(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        try {
            logFile?.appendText("[$level] $tag: $message\n")
            throwable?.let {
                logFile?.appendText("${it.stackTraceToString()}\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write to log file", e)
        }
    }

    fun getBuildLogs(projectId: String): String? {
        return try {
            logFile?.readText()
        } catch (e: Exception) {
            null
        }
    }

    fun clearLogs() {
        try {
            logFile?.writeText("")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear log file", e)
        }
    }
}

/**
 * Build log stream collector
 */
class BuildLogCollector {
    private val logs = mutableListOf<String>()

    fun addLog(line: String) {
        logs.add(line)
    }

    fun getLogs(): String = logs.joinToString("\n")

    fun getLogsAsList(): List<String> = logs.toList()

    fun clear() {
        logs.clear()
    }

    fun toFormattedString(): String {
        return logs.mapIndexed { index, line ->
            "[${index + 1}] $line"
        }.joinToString("\n")
    }
}
