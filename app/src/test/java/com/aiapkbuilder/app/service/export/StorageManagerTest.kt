package com.aiapkbuilder.app.service.export

import android.content.Context
import com.aiapkbuilder.app.data.local.BackupRecordDao
import com.aiapkbuilder.app.data.local.BuildHistoryDao
import com.aiapkbuilder.app.data.service.build.ArtifactCache
import com.aiapkbuilder.app.data.service.export.StorageManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class StorageManagerTest {

    private lateinit var storageManager: StorageManager
    private lateinit var backupRecordDao: BackupRecordDao
    private lateinit var buildHistoryDao: BuildHistoryDao
    private lateinit var artifactCache: ArtifactCache
    private lateinit var context: Context

    @Before
    fun setup() {
        backupRecordDao = mockk()
        buildHistoryDao = mockk()
        artifactCache = mockk()
        context = mockk()
        coEvery { context.cacheDir } returns File(System.getProperty("java.io.tmpdir"))
        storageManager = StorageManager(backupRecordDao, buildHistoryDao, artifactCache, context)
    }

    @Test
    fun `getStorageStats returns stats`() = runTest {
        coEvery { artifactCache.getCacheSize() } returns 1000L
        coEvery { artifactCache.getCacheStats() } returns mapOf("fileCount" to 5)

        val result = storageManager.getStorageStats()
        assertTrue(result.isSuccess)
        val stats = result.getOrNull()!!
        assertTrue(stats.totalArtifactSize >= 0)
        assertTrue(stats.artifactCount >= 0)
    }

    @Test
    fun `cleanupOldArtifacts executes without error`() = runTest {
        storageManager.cleanupOldArtifacts(30)
        assertTrue(true)
    }

    @Test
    fun `clearAllExports executes without error`() = runTest {
        storageManager.clearAllExports()
        assertTrue(true)
    }

    @Test
    fun `getTotalSize returns value`() = runTest {
        coEvery { artifactCache.getCacheSize() } returns 1000L
        coEvery { artifactCache.getCacheStats() } returns mapOf("fileCount" to 5)

        val size = storageManager.getTotalSize()
        assertTrue(size >= 0)
    }
}
