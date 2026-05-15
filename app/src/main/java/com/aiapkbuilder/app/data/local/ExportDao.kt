package com.aiapkbuilder.app.data.local

import androidx.room.*
import com.aiapkbuilder.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadSessionDao {
    @Query("SELECT * FROM download_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<DownloadSession>>

    @Query("SELECT * FROM download_sessions WHERE sessionId = :sessionId")
    suspend fun getSession(sessionId: String): DownloadSession?

    @Query("SELECT * FROM download_sessions WHERE status = :status ORDER BY startedAt DESC")
    fun getSessionsByStatus(status: DownloadStatus): Flow<List<DownloadSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DownloadSession)

    @Update
    suspend fun updateSession(session: DownloadSession)

    @Query("DELETE FROM download_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM download_sessions WHERE status = :status")
    suspend fun deleteSessionsByStatus(status: DownloadStatus)
}

@Dao
interface ShareLinkDao {
    @Query("SELECT * FROM share_links WHERE artifactId = :artifactId ORDER BY createdAt DESC")
    fun getLinksForArtifact(artifactId: String): Flow<List<ShareLink>>

    @Query("SELECT * FROM share_links WHERE linkId = :linkId")
    suspend fun getLink(linkId: String): ShareLink?

    @Query("SELECT * FROM share_links WHERE token = :token")
    suspend fun getLinkByToken(token: String): ShareLink?

    @Query("SELECT * FROM share_links WHERE isActive = 1 AND (expiresAt IS NULL OR expiresAt > :now)")
    fun getActiveLinks(now: Long = System.currentTimeMillis()): Flow<List<ShareLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: ShareLink)

    @Update
    suspend fun updateLink(link: ShareLink)

    @Query("DELETE FROM share_links WHERE linkId = :linkId")
    suspend fun deleteLink(linkId: String)

    @Query("DELETE FROM share_links WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredLinks(now: Long = System.currentTimeMillis())
}

@Dao
interface BackupRecordDao {
    @Query("SELECT * FROM backup_records WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getBackupsForProject(projectId: String): Flow<List<BackupRecord>>

    @Query("SELECT * FROM backup_records WHERE backupId = :backupId")
    suspend fun getBackup(backupId: String): BackupRecord?

    @Query("SELECT * FROM backup_records WHERE status = :status ORDER BY createdAt DESC")
    fun getBackupsByStatus(status: BackupStatus): Flow<List<BackupRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupRecord)

    @Update
    suspend fun updateBackup(backup: BackupRecord)

    @Query("DELETE FROM backup_records WHERE backupId = :backupId")
    suspend fun deleteBackup(backupId: String)

    @Query("SELECT SUM(fileSize) FROM backup_records WHERE status = :status")
    fun getTotalBackupSize(status: BackupStatus = BackupStatus.COMPLETED): Flow<Long?>
}

@Dao
interface BuildHistoryDao {
    @Query("SELECT * FROM build_history WHERE projectId = :projectId ORDER BY builtAt DESC")
    fun getHistoryForProject(projectId: String): Flow<List<BuildHistory>>

    @Query("SELECT * FROM build_history WHERE historyId = :historyId")
    suspend fun getHistory(historyId: String): BuildHistory?

    @Query("SELECT * FROM build_history WHERE projectId = :projectId AND builtAt >= :since ORDER BY builtAt DESC")
    fun getRecentHistory(projectId: String, since: Long): Flow<List<BuildHistory>>

    @Query("SELECT * FROM build_history WHERE projectId = :projectId AND success = 1 ORDER BY builtAt DESC LIMIT :limit")
    suspend fun getSuccessfulBuilds(projectId: String, limit: Int = 10): List<BuildHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: BuildHistory)

    @Update
    suspend fun updateHistory(history: BuildHistory)

    @Query("DELETE FROM build_history WHERE historyId = :historyId")
    suspend fun deleteHistory(historyId: String)

    @Query("SELECT COUNT(*) FROM build_history WHERE projectId = :projectId")
    fun getHistoryCount(projectId: String): Flow<Int>
}