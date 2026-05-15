package com.aiapkbuilder.app.data.repository

import com.aiapkbuilder.app.data.api.*
import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.data.local.*
import com.aiapkbuilder.app.util.AICodeGenerator
import com.aiapkbuilder.app.util.ProjectGenerationUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val buildJobDao: BuildJobDao,
    private val templateDao: TemplateDao,
    private val buildConfigDao: BuildConfigDao,
    private val artifactDao: ArtifactDao,
    private val aiCodeGenerator: AICodeGenerator,
    private val gson: Gson
) {

    // ─── Project CRUD Operations ───────────────────────────────
    fun getAllProjects(): Flow<List<AppProject>> = projectDao.getAllProjects()

    fun getProject(id: String): Flow<AppProject?> = projectDao.getProjectById(id)

    suspend fun getProjectByIdOnce(id: String): AppProject? = projectDao.getProjectByIdOnce(id)

    fun getFavoriteProjects(): Flow<List<AppProject>> = projectDao.getFavoriteProjects()

    fun getRecentProjects(limit: Int = 10): Flow<List<AppProject>> = projectDao.getRecentProjects(limit)

    fun getProjectsByType(type: AppType): Flow<List<AppProject>> = projectDao.getProjectsByType(type)

    suspend fun toggleFavorite(projectId: String, isFavorite: Boolean) =
        projectDao.toggleFavorite(projectId, isFavorite)

    suspend fun clearBuildLogs(projectId: String) {
        val project = projectDao.getProjectByIdOnce(projectId) ?: return
        projectDao.updateProject(project.copy(buildLogs = ""))
    }

    // ─── Project Generation ───────────────────────────────────
    suspend fun generateAndCreateProject(
        request: GenerationRequest,
        buildProvider: BuildProvider,
        onProgress: (Int, String) -> Unit
    ): String {
        val projectId = ProjectGenerationUtils.generateProjectId()

        // Step 0 — create placeholder
        val placeholder = AppProject(
            id = projectId,
            name = "Generating…",
            description = request.prompt,
            prompt = request.prompt,
            appType = request.appType ?: AppType.CUSTOM,
            buildStatus = BuildStatus.GENERATING,
            buildProvider = buildProvider
        )
        projectDao.insertProject(placeholder)
        onProgress(0, "Analyzing your idea...")

        // Step 1 — AI analysis
        val plan: GeneratedProjectPlan = try {
            aiCodeGenerator.generateProjectPlan(request)
        } catch (e: Exception) {
            fallbackPlan(request)
        }
        onProgress(2, "Planning app structure...")

        // Step 2 — update with plan
        val updatedProject = placeholder.copy(
            name = plan.appName,
            description = plan.description,
            packageName = plan.packageName,
            appType = plan.appType,
            features = plan.features,
            screens = plan.screens.map { it.name },
            buildStatus = BuildStatus.BUILDING,
            iconColor = plan.colorPrimary,
            generatedProjectPlan = plan
        )
        projectDao.updateProject(updatedProject)
        onProgress(4, "Generating code...")

        // Step 3 — generate source
        aiCodeGenerator.generateProjectSources(plan, projectId)
        onProgress(6, "Launching build pipeline...")

        // Step 4 — trigger build provider
        val finalStatus = when (buildProvider) {
            BuildProvider.LOCAL -> BuildStatus.SUCCESS
            else -> BuildStatus.BUILDING
        }

        projectDao.updateProject(updatedProject.copy(
            buildStatus = finalStatus,
            updatedAt = System.currentTimeMillis()
        ))

        return projectId
    }

    // ─── Build Management ──────────────────────────────────────
    fun getBuildJobsForProject(projectId: String): Flow<List<BuildJob>> =
        buildJobDao.getBuildJobsForProject(projectId)

    fun getLatestBuildJob(projectId: String): Flow<BuildJob?> =
        buildJobDao.getLatestBuildJob(projectId)

    suspend fun createBuildJob(job: BuildJob) =
        buildJobDao.insertBuildJob(job)

    suspend fun updateBuildJob(job: BuildJob) =
        buildJobDao.updateBuildJob(job)

    suspend fun retryBuild(projectId: String) {
        val project = projectDao.getProjectByIdOnce(projectId) ?: return
        projectDao.updateProject(project.copy(
            buildStatus = BuildStatus.BUILDING,
            errorMessage = null,
            totalBuilds = project.totalBuilds + 1,
            updatedAt = System.currentTimeMillis()
        ))
    }

    suspend fun getBuildJob(jobId: String): BuildJob? =
        buildJobDao.getBuildJob(jobId)

    // ─── Build Configuration ───────────────────────────────────
    fun getBuildConfigsForProject(projectId: String): Flow<List<BuildConfig>> =
        buildConfigDao.getConfigsForProject(projectId)

    suspend fun saveBuildConfig(config: BuildConfig) =
        buildConfigDao.insertBuildConfig(config)

    suspend fun getBuildConfigForProvider(projectId: String, provider: BuildProvider): BuildConfig? =
        buildConfigDao.getConfigForProjectAndProvider(projectId, provider)

    // ─── Artifacts ────────────────────────────────────────────
    fun getArtifactsForProject(projectId: String): Flow<List<BuildArtifact>> =
        artifactDao.getArtifactsForProject(projectId)

    fun getArtifactsForBuild(buildJobId: String): Flow<List<BuildArtifact>> =
        artifactDao.getArtifactsForBuild(buildJobId)

    fun getTotalArtifactSize(projectId: String): Flow<Long?> =
        artifactDao.getTotalArtifactSize(projectId)

    suspend fun saveArtifact(artifact: BuildArtifact) =
        artifactDao.insertArtifact(artifact)

    suspend fun deleteArtifact(artifactId: String) =
        artifactDao.deleteArtifact(artifactId)

    // ─── Project Deletion ──────────────────────────────────────
    suspend fun deleteProject(projectId: String) {
        projectDao.deleteProject(projectId)
        buildJobDao.deleteBuildJobsForProject(projectId)
        // Cleanup artifacts, build configs, etc.
    }

    // ─── Fallback/Demo Plan ────────────────────────────────────
    private fun fallbackPlan(request: GenerationRequest) = GeneratedProjectPlan(
        appName = extractAppName(request.prompt),
        packageName = "com.generated.${extractPackage(request.prompt)}",
        description = request.prompt,
        appType = request.appType ?: AppType.CUSTOM,
        screens = listOf(
            ScreenSpec("Home", "/home", "Main screen", listOf("TopAppBar", "LazyColumn")),
            ScreenSpec("Detail", "/detail", "Detail view", listOf("Card", "Button")),
            ScreenSpec("Settings", "/settings", "App settings", listOf("Switch", "TextField"))
        ),
        features = request.additionalFeatures.ifEmpty {
            listOf("Dark Mode", "Material3 Design", "Navigation")
        },
        dependencies = listOf("compose", "hilt", "room", "retrofit"),
        colorPrimary = "#6200EA",
        colorSecondary = "#03DAC6",
        minSdkVersion = 26,
        permissions = listOf("INTERNET"),
        navigationStructure = "Bottom Navigation",
        databaseTables = listOf("items"),
        apiEndpoints = emptyList()
    )

    private fun extractAppName(prompt: String): String {
        val words = prompt.split(" ")
        return when {
            words.size >= 3 -> "${words[2].replaceFirstChar { it.uppercase() }} App"
            else -> "My AI App"
        }
    }

    private fun extractPackage(prompt: String): String =
        prompt.lowercase().replace(Regex("[^a-z0-9]"), "").take(10).ifEmpty { "myapp" }

    suspend fun generateProjectPlan(request: GenerationRequest): GeneratedProjectPlan {
        return aiCodeGenerator.generateProjectPlan(request)
    }
}