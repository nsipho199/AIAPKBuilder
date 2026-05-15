package com.aiapkbuilder.app.data.local

import androidx.room.*
import com.aiapkbuilder.app.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<AppProject>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: String): Flow<AppProject?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdOnce(id: String): AppProject?

    @Query("SELECT * FROM projects WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavoriteProjects(): Flow<List<AppProject>>

    @Query("SELECT * FROM projects WHERE appType = :type ORDER BY updatedAt DESC")
    fun getProjectsByType(type: AppType): Flow<List<AppProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AppProject)

    @Update
    suspend fun updateProject(project: AppProject)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>

    @Query("UPDATE projects SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)

    @Query("SELECT * FROM projects ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentProjects(limit: Int = 10): Flow<List<AppProject>>
}

@Dao
interface BuildJobDao {
    @Query("SELECT * FROM build_jobs WHERE projectId = :projectId ORDER BY startedAt DESC")
    fun getBuildJobsForProject(projectId: String): Flow<List<BuildJob>>

    @Query("SELECT * FROM build_jobs WHERE projectId = :projectId ORDER BY startedAt DESC LIMIT 1")
    fun getLatestBuildJob(projectId: String): Flow<BuildJob?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildJob(job: BuildJob)

    @Update
    suspend fun updateBuildJob(job: BuildJob)

    @Query("SELECT * FROM build_jobs WHERE jobId = :jobId")
    suspend fun getBuildJob(jobId: String): BuildJob?

    @Query("DELETE FROM build_jobs WHERE projectId = :projectId")
    suspend fun deleteBuildJobsForProject(projectId: String)
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates WHERE isBuiltIn = 1 ORDER BY name ASC")
    fun getBuiltInTemplates(): Flow<List<ProjectTemplate>>

    @Query("SELECT * FROM templates WHERE appType = :type ORDER BY difficulty ASC")
    fun getTemplatesByType(type: AppType): Flow<List<ProjectTemplate>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: String): ProjectTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ProjectTemplate)

    @Update
    suspend fun updateTemplate(template: ProjectTemplate)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("SELECT COUNT(*) FROM templates")
    fun getTemplateCount(): Flow<Int>
}

@Dao
interface BuildConfigDao {
    @Query("SELECT * FROM build_configs WHERE projectId = :projectId")
    fun getConfigsForProject(projectId: String): Flow<List<BuildConfig>>

    @Query("SELECT * FROM build_configs WHERE projectId = :projectId AND provider = :provider")
    suspend fun getConfigForProjectAndProvider(projectId: String, provider: BuildProvider): BuildConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildConfig(config: BuildConfig)

    @Update
    suspend fun updateBuildConfig(config: BuildConfig)

    @Query("DELETE FROM build_configs WHERE id = :id")
    suspend fun deleteBuildConfig(id: String)
}

@Dao
interface CodeCacheDao {
    @Query("SELECT * FROM code_cache WHERE projectId = :projectId AND screenName = :screenName AND codeType = :codeType")
    suspend fun getCodeFromCache(projectId: String, screenName: String, codeType: String): CodeGenerationCache?

    @Query("SELECT * FROM code_cache WHERE projectId = :projectId ORDER BY generatedAt DESC")
    fun getAllCacheForProject(projectId: String): Flow<List<CodeGenerationCache>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCode(code: CodeGenerationCache)

    @Query("DELETE FROM code_cache WHERE expiresAt < :now")
    suspend fun deleteExpiredCache(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM code_cache WHERE projectId = :projectId")
    suspend fun clearProjectCache(projectId: String)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE buildJobId = :buildJobId")
    fun getArtifactsForBuild(buildJobId: String): Flow<List<BuildArtifact>>

    @Query("SELECT * FROM artifacts WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getArtifactsForProject(projectId: String): Flow<List<BuildArtifact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: BuildArtifact)

    @Update
    suspend fun updateArtifact(artifact: BuildArtifact)

    @Query("DELETE FROM artifacts WHERE id = :id")
    suspend fun deleteArtifact(id: String)

    @Query("SELECT SUM(fileSizeBytes) FROM artifacts WHERE projectId = :projectId")
    fun getTotalArtifactSize(projectId: String): Flow<Long?>
}

@Database(
    entities = [
        AppProject::class,
        BuildJob::class,
        ProjectTemplate::class,
        BuildConfig::class,
        CodeGenerationCache::class,
        BuildArtifact::class,
        DownloadSession::class,
        ShareLink::class,
        BackupRecord::class,
        BuildHistory::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(StringListConverter::class, MapConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun buildJobDao(): BuildJobDao
    abstract fun templateDao(): TemplateDao
    abstract fun buildConfigDao(): BuildConfigDao
    abstract fun codeCacheDao(): CodeCacheDao
    abstract fun artifactDao(): ArtifactDao
    abstract fun downloadSessionDao(): DownloadSessionDao
    abstract fun shareLinkDao(): ShareLinkDao
    abstract fun backupRecordDao(): BackupRecordDao
    abstract fun buildHistoryDao(): BuildHistoryDao

    companion object {
        const val DATABASE_NAME = "aiapkbuilder.db"
    }
}
