package com.aiapkbuilder.app.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiapkbuilder.app.data.model.AppProject
import com.aiapkbuilder.app.data.model.BuildJob
import com.aiapkbuilder.app.data.model.BuildStatus
import com.aiapkbuilder.app.data.model.ExportConfig
import com.aiapkbuilder.app.data.model.ShareConfig
import com.aiapkbuilder.app.data.model.ShareLink
import com.aiapkbuilder.app.data.repository.ProjectRepository
import com.aiapkbuilder.app.data.service.build.BuildExecutor
import com.aiapkbuilder.app.data.service.build.LogAggregator
import com.aiapkbuilder.app.data.service.export.BuildHistoryManager
import com.aiapkbuilder.app.data.service.export.DownloadManager
import com.aiapkbuilder.app.data.service.export.ProjectExporter
import com.aiapkbuilder.app.data.service.export.ShareService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProjectDetailUiState(
    val project: AppProject? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val buildJobs: List<BuildJob> = emptyList(),
    val currentBuildJob: BuildJob? = null,
    val buildLogs: List<String> = emptyList(),
    val isBuilding: Boolean = false,
    val buildProgress: Float = 0f,
    val buildMessage: String = "",
    val isExporting: Boolean = false,
    val isGeneratingShareLink: Boolean = false,
    val generatedShareLink: ShareLink? = null,
    val shareUrl: String = "",
    val qrCodeBitmap: Bitmap? = null,
    val historyCount: Int = 0
)

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val buildExecutor: BuildExecutor,
    private val logAggregator: LogAggregator,
    private val downloadManager: DownloadManager,
    private val projectExporter: ProjectExporter,
    private val shareService: ShareService,
    private val buildHistoryManager: BuildHistoryManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProjectDetailUiState())
    val uiState: StateFlow<ProjectDetailUiState> = _uiState.asStateFlow()

    private var currentProjectId: String = ""

    fun loadProject(projectId: String) {
        currentProjectId = projectId
        viewModelScope.launch {
            repository.getProject(projectId).collect { project ->
                _uiState.update { it.copy(project = project) }
            }
        }

        viewModelScope.launch {
            repository.getBuildJobsForProject(projectId).collect { jobs ->
                _uiState.update { it.copy(buildJobs = jobs) }
            }
        }

        viewModelScope.launch {
            repository.getLatestBuildJob(projectId).collect { job ->
                _uiState.update { it.copy(currentBuildJob = job) }
                job?.let { loadBuildLogs(it.jobId) }
            }
        }

        viewModelScope.launch {
            buildHistoryManager.getHistoryCount(projectId).collect { count ->
                _uiState.update { it.copy(historyCount = count) }
            }
        }
    }

    private fun loadBuildLogs(jobId: String) {
        viewModelScope.launch {
            val logs = logAggregator.getLogs(jobId)
            _uiState.update { it.copy(buildLogs = logs) }
        }
    }

    fun downloadApk() {
        val project = _uiState.value.project ?: return
        val apkPath = project.apkPath ?: return
        val file = File(apkPath)
        if (!file.exists()) return

        viewModelScope.launch {
            downloadManager.startDownload(
                artifactId = project.id,
                remoteUrl = "file://$apkPath",
                fileName = "${project.name}_v${project.versionName}.apk",
                totalSize = file.length()
            )
        }
    }

    fun exportProject(config: ExportConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = projectExporter.exportProject(config)
            _uiState.update { it.copy(isExporting = false) }
            result.onSuccess { zipFile ->
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Share Export").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
    }

    fun shareProject() {
        val project = _uiState.value.project ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out my app '${project.name}' built with AI APK Builder!")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Project").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun generateShareLink(config: ShareConfig) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingShareLink = true) }
            val result = shareService.generateShareLink(config)
            result.onSuccess { link ->
                val url = shareService.getShareUrl(link)
                val qrResult = shareService.generateQRCode(url)
                _uiState.update {
                    it.copy(
                        isGeneratingShareLink = false,
                        generatedShareLink = link,
                        shareUrl = url,
                        qrCodeBitmap = qrResult.getOrNull()
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isGeneratingShareLink = false, error = it.message) }
            }
        }
    }

    fun recordBuildHistory() {
        val project = _uiState.value.project ?: return
        val job = _uiState.value.currentBuildJob ?: return
        viewModelScope.launch {
            buildHistoryManager.recordBuild(
                projectId = project.id,
                buildJobId = job.jobId,
                versionName = project.versionName,
                versionCode = project.versionCode,
                sizeBytes = project.apkPath?.let { File(it).length() } ?: 0L,
                buildDurationMs = 0L,
                provider = project.buildProvider.displayName,
                success = project.buildStatus == BuildStatus.SUCCESS,
                tags = project.features,
                notes = project.description
            )
        }
    }

    fun startBuild() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isBuilding = true, buildProgress = 0f, buildMessage = "Starting build...") }

                val buildResult = buildExecutor.executeBuild(
                    projectId = project.id,
                    preferredProvider = project.buildProvider,
                    onProgress = { percent, message ->
                        _uiState.update { it.copy(
                            buildProgress = percent / 100f,
                            buildMessage = message
                        )}
                    }
                )

                if (buildResult.isSuccess) {
                    val job = buildResult.getOrNull()!!
                    loadBuildLogs(job.jobId)
                    _uiState.update { it.copy(isBuilding = false, buildProgress = 1f, buildMessage = "Build completed!") }
                    recordBuildHistory()
                } else {
                    _uiState.update { it.copy(
                        isBuilding = false,
                        error = buildResult.exceptionOrNull()?.message ?: "Build failed",
                        buildProgress = 0f
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isBuilding = false,
                    error = e.message,
                    buildProgress = 0f
                )}
            }
        }
    }

    fun cancelBuild() {
        val currentJob = _uiState.value.currentBuildJob ?: return
        viewModelScope.launch {
            try {
                val result = buildExecutor.cancelBuild(currentJob.jobId)
                if (result.isSuccess) {
                    _uiState.update { it.copy(buildMessage = "Build cancelled") }
                } else {
                    _uiState.update { it.copy(error = "Failed to cancel build") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun retryBuild() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, buildProgress = 0f, buildMessage = "Retrying build...") }

                val buildResult = buildExecutor.retryBuild(
                    projectId = project.id,
                    onProgress = { percent, message ->
                        _uiState.update { it.copy(
                            buildProgress = percent / 100f,
                            buildMessage = message
                        )}
                    }
                )

                if (buildResult.isSuccess) {
                    val job = buildResult.getOrNull()!!
                    loadBuildLogs(job.jobId)
                    _uiState.update { it.copy(isLoading = false, buildProgress = 1f, buildMessage = "Build completed!") }
                    recordBuildHistory()
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = buildResult.exceptionOrNull()?.message ?: "Build failed",
                        buildProgress = 0f
                    )}
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearBuildLogs() {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            repository.clearBuildLogs(project.id)
            _uiState.update { it.copy(buildLogs = emptyList()) }
        }
    }

    fun showDownloads() {
        val intent = Intent(context, com.aiapkbuilder.app.MainActivity::class.java).apply {
            putExtra("navigate_to", "downloads")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun navigateToHistory() {
        val intent = Intent(context, com.aiapkbuilder.app.MainActivity::class.java).apply {
            putExtra("navigate_to", "history")
            putExtra("project_id", currentProjectId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    init {
        // Load initial project details if needed
    }
}