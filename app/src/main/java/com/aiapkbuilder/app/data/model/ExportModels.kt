package com.aiapkbuilder.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.io.Serializable

// ─── Download Session ──────────────────────────────────────────
@Entity(tableName = "download_sessions")
data class DownloadSession(
    @PrimaryKey val sessionId: String,
    val artifactId: String,
    val fileName: String,
    val totalSize: Long,
    val remoteUrl: String = "",
    val downloadedSize: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val localPath: String? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val speed: Long = 0L, // bytes per second
    val etaMs: Long = 0L // estimated time remaining
) : Serializable

enum class DownloadStatus {
    PENDING, ACTIVE, PAUSED, COMPLETED, FAILED, CANCELLED
}

// ─── Share Link ────────────────────────────────────────────────
@Entity(tableName = "share_links")
data class ShareLink(
    @PrimaryKey val linkId: String,
    val artifactId: String,
    val token: String,
    val expiresAt: Long? = null, // null for permanent
    val passwordHash: String? = null,
    val maxDownloads: Int? = null,
    val downloadCount: Int = 0,
    val viewCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "user",
    val isActive: Boolean = true
) : Serializable

// ─── Backup Record ────────────────────────────────────────────
@Entity(tableName = "backup_records")
data class BackupRecord(
    @PrimaryKey val backupId: String,
    val projectId: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val status: BackupStatus = BackupStatus.PENDING,
    val storageProvider: String = "local", // local, google_drive, dropbox
    val remoteUrl: String? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

enum class BackupStatus {
    PENDING, IN_PROGRESS, COMPLETED, FAILED, RESTORING
}

// ─── Build History ────────────────────────────────────────────
@Entity(tableName = "build_history")
@TypeConverters(StringListConverter::class)
data class BuildHistory(
    @PrimaryKey val historyId: String,
    val projectId: String,
    val buildJobId: String,
    val versionName: String,
    val versionCode: Int,
    val builtAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L,
    val buildDurationMs: Long = 0L,
    val provider: String = "unknown",
    val success: Boolean = false,
    val downloadCount: Int = 0,
    val tags: List<String> = emptyList(),
    val notes: String = ""
) : Serializable

// ─── Storage Stats ────────────────────────────────────────────
data class StorageStats(
    val totalArtifactSize: Long = 0L,
    val totalBackupSize: Long = 0L,
    val totalCacheSize: Long = 0L,
    val artifactCount: Int = 0,
    val backupCount: Int = 0,
    val cacheCount: Int = 0,
    val oldestArtifactAge: Long = 0L, // days
    val recommendedCleanup: Long = 0L // bytes to free
) : Serializable

// ─── Download Progress ─────────────────────────────────────────
data class DownloadProgress(
    val sessionId: String,
    val fileName: String,
    val downloadedSize: Long,
    val totalSize: Long,
    val progressPercent: Int,
    val speed: Long, // bytes per second
    val etaSeconds: Long,
    val status: DownloadStatus,
    val errorMessage: String? = null
) : Serializable

// ─── Export Config ────────────────────────────────────────────
data class ExportConfig(
    val projectId: String,
    val exportType: ExportType = ExportType.BOTH, // source, binary, both
    val includeMetadata: Boolean = true,
    val includeHistory: Boolean = false,
    val includeResources: Boolean = true,
    val compressionLevel: Int = 6, // 1-9
    val fileName: String = "",
    val description: String = ""
) : Serializable

enum class ExportType {
    SOURCE, BINARY, BOTH
}

// ─── Share Config ─────────────────────────────────────────────
data class ShareConfig(
    val artifactId: String,
    val shareType: ShareType = ShareType.LINK,
    val expirationDays: Int? = 7, // null for permanent
    val password: String? = null,
    val maxDownloads: Int? = null,
    val includeMetadata: Boolean = true,
    val customMessage: String = ""
) : Serializable

enum class ShareType {
    LINK, QR_CODE, EMAIL, SOCIAL_MEDIA
}