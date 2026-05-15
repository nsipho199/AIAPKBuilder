package com.aiapkbuilder.app.data.service

import android.content.Context
import androidx.work.*
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.service.build.BuildExecutor
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager worker for handling background build operations
 */
class BuildWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var projectRepository: ProjectRepository

    @Inject
    lateinit var buildExecutor: BuildExecutor

    @Inject
    lateinit var gson: Gson

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val projectId = inputData.getString(KEY_PROJECT_ID) ?: return@withContext Result.retry()

            // Use BuildExecutor to handle the build
            val buildResult = buildExecutor.executeBuild(
                projectId = projectId,
                onProgress = { percent, message ->
                    setProgress(
                        workDataOf(
                            KEY_PROGRESS to percent.toInt(),
                            KEY_STATUS to message
                        )
                    )
                }
            )

            if (buildResult.isSuccess) {
                Result.success(workDataOf(KEY_BUILD_JOB_ID to buildResult.getOrNull()!!.jobId))
            } else {
                val error = buildResult.exceptionOrNull()
                Result.failure(workDataOf(KEY_ERROR to (error?.message ?: "Build failed")))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_BUILD_JOB_ID = "build_job_id"
        const val KEY_BUILD_PROVIDER = "build_provider"
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"

        fun buildBuildRequest(
            projectId: String,
            buildJobId: String,
            provider: String
        ): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_PROJECT_ID to projectId,
                KEY_BUILD_JOB_ID to buildJobId,
                KEY_BUILD_PROVIDER to provider
            )

            return OneTimeWorkRequestBuilder<BuildWorker>()
                .setInputData(inputData)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()
        }
    }
}

/**
 * Service for managing build job scheduling
 */
@Singleton
class BuildJobScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository
) {
    private val workManager = WorkManager.getInstance(context)

    suspend fun scheduleBuild(
        projectId: String,
        buildJobId: String,
        provider: String
    ) {
        val buildRequest = BuildWorker.buildBuildRequest(projectId, buildJobId, provider)
        workManager.enqueueUniqueWork(
            "build_$buildJobId",
            ExistingWorkPolicy.KEEP,
            buildRequest
        )
    }

    fun observeBuildProgress(buildJobId: String) =
        workManager.getWorkInfoByIdLiveData(java.util.UUID.randomUUID())

    suspend fun cancelBuild(buildJobId: String) {
        workManager.cancelAllWorkByTag("build_$buildJobId")
    }
}