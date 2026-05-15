package com.aiapkbuilder.app.data.repository

import com.aiapkbuilder.app.data.api.GitHubActionsApiService
import com.aiapkbuilder.app.data.local.ProjectDao
import com.aiapkbuilder.app.data.local.BuildJobDao
import com.aiapkbuilder.app.data.local.TemplateDao
import com.aiapkbuilder.app.data.local.BuildConfigDao
import com.aiapkbuilder.app.data.local.ArtifactDao
import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.util.AICodeGenerator
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProjectRepositoryTest {

    private lateinit var repository: ProjectRepository
    private lateinit var projectDao: ProjectDao
    private lateinit var buildJobDao: BuildJobDao
    private lateinit var templateDao: TemplateDao
    private lateinit var buildConfigDao: BuildConfigDao
    private lateinit var artifactDao: ArtifactDao
    private lateinit var aiCodeGenerator: AICodeGenerator

    @Before
    fun setup() {
        projectDao = mockk()
        buildJobDao = mockk()
        templateDao = mockk()
        buildConfigDao = mockk()
        artifactDao = mockk()
        aiCodeGenerator = mockk()
        repository = ProjectRepository(projectDao, buildJobDao, templateDao, buildConfigDao, artifactDao, aiCodeGenerator, Gson())
    }

    @Test
    fun `getAllProjects returns flow from dao`() {
        val projects = listOf(AppProject(id = "1", name = "Test", description = "d", prompt = "p", appType = AppType.CUSTOM))
        coEvery { projectDao.getAllProjects() } returns flowOf(projects)
        val result = repository.getAllProjects()
        assertNotNull(result)
    }

    @Test
    fun `toggleFavorite delegates to dao`() = runTest {
        coEvery { projectDao.toggleFavorite("1", true) } returns Unit
        repository.toggleFavorite("1", true)
        coVerify { projectDao.toggleFavorite("1", true) }
    }

    @Test
    fun `clearBuildLogs updates project with empty logs`() = runTest {
        val project = AppProject(id = "1", name = "Test", description = "d", prompt = "p", appType = AppType.CUSTOM, buildLogs = "some logs")
        coEvery { projectDao.getProjectByIdOnce("1") } returns project
        coEvery { projectDao.updateProject(any()) } returns Unit
        repository.clearBuildLogs("1")
        coVerify { projectDao.updateProject(match { it.buildLogs == "" }) }
    }

    @Test
    fun `retryBuild resets project state`() = runTest {
        val project = AppProject(id = "1", name = "Test", description = "d", prompt = "p", appType = AppType.CUSTOM, buildStatus = BuildStatus.FAILED, totalBuilds = 1)
        coEvery { projectDao.getProjectByIdOnce("1") } returns project
        coEvery { projectDao.updateProject(any()) } returns Unit
        repository.retryBuild("1")
        coVerify { projectDao.updateProject(match { it.buildStatus == BuildStatus.BUILDING && it.totalBuilds == 2 }) }
    }

    @Test
    fun `deleteProject cleans up build jobs`() = runTest {
        coEvery { projectDao.deleteProject("1") } returns Unit
        coEvery { buildJobDao.deleteBuildJobsForProject("1") } returns Unit
        repository.deleteProject("1")
        coVerify { projectDao.deleteProject("1") }
        coVerify { buildJobDao.deleteBuildJobsForProject("1") }
    }

    @Test
    fun `saveBuildConfig delegates to dao`() = runTest {
        val config = BuildConfig(id = "c1", projectId = "p1", provider = BuildProvider.DOCKER, configJson = "{}")
        coEvery { buildConfigDao.insertBuildConfig(config) } returns Unit
        repository.saveBuildConfig(config)
        coVerify { buildConfigDao.insertBuildConfig(config) }
    }

    @Test
    fun `saveArtifact delegates to dao`() = runTest {
        val artifact = BuildArtifact(id = "a1", buildJobId = "j1", projectId = "p1", artifactType = "apk", fileName = "app.apk", fileSizeBytes = 1024)
        coEvery { artifactDao.insertArtifact(artifact) } returns Unit
        repository.saveArtifact(artifact)
        coVerify { artifactDao.insertArtifact(artifact) }
    }

    @Test
    fun `extractAppName parses prompt correctly`() {
        val name = repository.javaClass.getDeclaredMethod("extractAppName", String::class.java).apply { isAccessible = true }.invoke(repository, "I want a weather app")
        assertEquals("Weather App", name)
    }

    @Test
    fun `extractAppName fallback for short prompts`() {
        val name = repository.javaClass.getDeclaredMethod("extractAppName", String::class.java).apply { isAccessible = true }.invoke(repository, "Hi")
        assertEquals("My AI App", name)
    }
}
