package com.aiapkbuilder.app.service.export

import com.aiapkbuilder.app.data.local.DownloadSessionDao
import com.aiapkbuilder.app.data.model.DownloadSession
import com.aiapkbuilder.app.data.model.DownloadStatus
import com.aiapkbuilder.app.data.service.export.DownloadManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DownloadManagerTest {

    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadSessionDao: DownloadSessionDao

    @Before
    fun setup() {
        downloadSessionDao = mockk()
        downloadManager = DownloadManager(downloadSessionDao)
    }

    @Test
    fun `startDownload returns valid session`() = runTest {
        coEvery { downloadSessionDao.insertSession(any()) } returns Unit

        val result = downloadManager.startDownload("a1", "https://example.com/app.apk", "Test App.apk", 1024000)
        assertTrue(result.isSuccess)
        val session = result.getOrNull()!!
        assertEquals("a1", session.artifactId)
        assertEquals(DownloadStatus.PENDING, session.status)
        assertEquals("Test App.apk", session.fileName)
    }

    @Test
    fun `startDownload handles errors gracefully`() = runTest {
        coEvery { downloadSessionDao.insertSession(any()) } throws RuntimeException("DB error")

        val result = downloadManager.startDownload("a1", "https://example.com/app.apk", "fail.apk", 100)
        assertTrue(result.isFailure)
    }

    @Test
    fun `pauseDownload pauses active session`() = runTest {
        val session = DownloadSession(sessionId = "s1", artifactId = "a1", fileName = "test.apk", totalSize = 100, remoteUrl = "https://example.com/test.apk", status = DownloadStatus.ACTIVE)
        coEvery { downloadSessionDao.getSession("s1") } returns session
        coEvery { downloadSessionDao.updateSession(any()) } returns Unit

        val result = downloadManager.pauseDownload("s1")
        assertTrue(result.isSuccess)
        coVerify { downloadSessionDao.updateSession(match { it.status == DownloadStatus.PAUSED }) }
    }

    @Test
    fun `pauseDownload ignores non-active sessions`() = runTest {
        val session = DownloadSession(sessionId = "s1", artifactId = "a1", fileName = "test.apk", totalSize = 100, remoteUrl = "https://example.com/test.apk", status = DownloadStatus.COMPLETED)
        coEvery { downloadSessionDao.getSession("s1") } returns session

        val result = downloadManager.pauseDownload("s1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `cancelDownload cancels and cleans up`() = runTest {
        val session = DownloadSession(sessionId = "s1", artifactId = "a1", fileName = "test.apk", totalSize = 100, remoteUrl = "https://example.com/test.apk", status = DownloadStatus.ACTIVE)
        coEvery { downloadSessionDao.getSession("s1") } returns session
        coEvery { downloadSessionDao.updateSession(any()) } returns Unit

        val result = downloadManager.cancelDownload("s1")
        assertTrue(result.isSuccess)
        coVerify { downloadSessionDao.updateSession(match { it.status == DownloadStatus.CANCELLED }) }
    }

    @Test
    fun `getAllSessions returns flow`() {
        coEvery { downloadSessionDao.getAllSessions() } returns flowOf(emptyList())
        val result = downloadManager.getAllSessions()
        assertNotNull(result)
    }

    @Test
    fun `getSession returns session by id`() = runTest {
        val session = DownloadSession(sessionId = "s1", artifactId = "a1", fileName = "test.apk", totalSize = 100, remoteUrl = "https://example.com/test.apk")
        coEvery { downloadSessionDao.getSession("s1") } returns session
        val result = downloadManager.getSession("s1")
        assertEquals("s1", result?.sessionId)
    }

    @Test
    fun `clearCompleted deletes completed sessions`() = runTest {
        coEvery { downloadSessionDao.deleteSessionsByStatus(DownloadStatus.COMPLETED) } returns Unit
        downloadManager.clearCompleted()
        coVerify { downloadSessionDao.deleteSessionsByStatus(DownloadStatus.COMPLETED) }
    }
}
