package com.aiapkbuilder.app.data.service.export

import com.aiapkbuilder.app.data.local.DownloadSessionDao
import com.aiapkbuilder.app.data.model.DownloadProgress
import com.aiapkbuilder.app.data.model.DownloadSession
import com.aiapkbuilder.app.data.model.DownloadStatus
import com.aiapkbuilder.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val downloadSessionDao: DownloadSessionDao
) {
    private val logger = AppLogger.getLogger("DownloadManager")
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: Flow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()

    suspend fun startDownload(
        artifactId: String,
        remoteUrl: String,
        fileName: String,
        totalSize: Long
    ): Result<DownloadSession> = withContext(Dispatchers.IO) {
        try {
            val session = DownloadSession(
                sessionId = UUID.randomUUID().toString(),
                artifactId = artifactId,
                remoteUrl = remoteUrl,
                fileName = fileName,
                totalSize = totalSize,
                status = DownloadStatus.PENDING,
                startedAt = System.currentTimeMillis()
            )
            downloadSessionDao.insertSession(session)
            // Launch download asynchronously
            GlobalScope.launch(Dispatchers.IO) {
                launchDownloadWorker(session)
            }
            Result.success(session)
        } catch (e: Exception) {
            logger.e("Failed to start download", e)
            Result.failure(e)
        }
    }

    suspend fun pauseDownload(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = downloadSessionDao.getSession(sessionId)
            if (session != null && session.status == DownloadStatus.ACTIVE) {
                downloadSessionDao.updateSession(session.copy(status = DownloadStatus.PAUSED))
                _activeDownloads.value = _activeDownloads.value - sessionId
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resumeDownload(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = downloadSessionDao.getSession(sessionId)
            if (session != null && session.status == DownloadStatus.PAUSED) {
                downloadSessionDao.updateSession(session.copy(status = DownloadStatus.ACTIVE))
                // Launch download asynchronously
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    launchDownloadWorker(session)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelDownload(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val session = downloadSessionDao.getSession(sessionId)
            if (session != null) {
                downloadSessionDao.updateSession(session.copy(status = DownloadStatus.CANCELLED, completedAt = System.currentTimeMillis()))
                session.localPath?.let { File(it).delete() }
                _activeDownloads.value = _activeDownloads.value - sessionId
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeProgress(sessionId: String): Flow<DownloadProgress> {
        return flow {
            val current = _activeDownloads.value[sessionId]
            if (current != null) emit(current)
        }
    }

    fun getAllSessions(): Flow<List<DownloadSession>> = downloadSessionDao.getAllSessions()

    suspend fun getSession(sessionId: String): DownloadSession? = downloadSessionDao.getSession(sessionId)

    private suspend fun launchDownloadWorker(session: DownloadSession) = withContext(Dispatchers.IO) {
        try {
            downloadSessionDao.updateSession(session.copy(status = DownloadStatus.ACTIVE))

            val downloadFile = File.createTempFile("download_", "_${session.fileName}")
            val outputStream = FileOutputStream(downloadFile, session.downloadedSize > 0)

            val totalSize: Long
            val inputStream = if (session.remoteUrl.startsWith("file://")) {
                val file = File(session.remoteUrl.substring(7))
                totalSize = file.length()
                FileInputStream(file).also { it.skip(session.downloadedSize) }
            } else {
                val url = URL(session.remoteUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000

                if (session.downloadedSize > 0) {
                    connection.setRequestProperty("Range", "bytes=${session.downloadedSize}-")
                }

                connection.connect()
                totalSize = connection.contentLengthLong
                connection.inputStream
            }

            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalRead = session.downloadedSize
                val startTime = System.currentTimeMillis()

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    val currentSession = downloadSessionDao.getSession(session.sessionId)
                    if (currentSession == null || currentSession.status == DownloadStatus.CANCELLED) break
                    if (currentSession.status == DownloadStatus.PAUSED) {
                        downloadSessionDao.updateSession(currentSession.copy(
                            downloadedSize = totalRead,
                            localPath = downloadFile.absolutePath
                        ))
                        return@withContext
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val elapsed = System.currentTimeMillis() - startTime
                    val speed = if (elapsed > 0) (totalRead * 1000L) / elapsed else 0L
                    val eta = if (speed > 0 && totalSize > 0) (totalSize - totalRead) / speed else 0L
                    val progress = DownloadProgress(
                        sessionId = session.sessionId,
                        fileName = session.fileName,
                        downloadedSize = totalRead,
                        totalSize = totalSize,
                        progressPercent = if (totalSize > 0) ((totalRead * 100) / totalSize).toInt() else 0,
                        speed = speed,
                        etaSeconds = eta,
                        status = DownloadStatus.ACTIVE
                    )
                    _activeDownloads.value = _activeDownloads.value + (session.sessionId to progress)
                }
            }

            outputStream.close()

            val completedSession = downloadSessionDao.getSession(session.sessionId)
            if (completedSession?.status == DownloadStatus.ACTIVE || completedSession?.status == DownloadStatus.PENDING) {
                downloadSessionDao.updateSession(completedSession.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadedSize = downloadFile.length(),
                    localPath = downloadFile.absolutePath,
                    completedAt = System.currentTimeMillis()
                ))
            }
        } catch (e: Exception) {
            logger.e("Download failed", e)
            downloadSessionDao.updateSession(session.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message,
                completedAt = System.currentTimeMillis()
            ))
        } finally {
            _activeDownloads.value = _activeDownloads.value - session.sessionId
        }
    }

    suspend fun clearCompleted() = withContext(Dispatchers.IO) {
        downloadSessionDao.deleteSessionsByStatus(DownloadStatus.COMPLETED)
    }
}