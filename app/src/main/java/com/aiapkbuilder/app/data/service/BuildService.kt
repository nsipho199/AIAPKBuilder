package com.aiapkbuilder.app.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.aiapkbuilder.app.R
import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.repository.ProjectRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@HiltWorker
class BuildWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val projectRepository: ProjectRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val PROJECT_ID_KEY = "project_id"
        const val NOTIFICATION_CHANNEL_ID = "build_notifications"

        fun buildRequest(projectId: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<BuildWorker>()
                .setInputData(workDataOf(PROJECT_ID_KEY to projectId))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val projectId = inputData.getString(PROJECT_ID_KEY) ?: return@withContext Result.failure()
        setForeground(createForegroundInfo("Starting build..."))

        try {
            // Poll build status until complete
            repeat(120) { attempt ->
                setForeground(createForegroundInfo("Building APK... (${attempt}s)"))
                delay(5000)
                // In real impl: poll build provider API here
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to (e.message ?: "Unknown error")))
        }
    }

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val notificationManager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Build Notifications",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("AI APK Builder")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1001, notification)
    }
}

// Stub Service class referenced in manifest
class BuildService : android.app.Service() {
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
}
