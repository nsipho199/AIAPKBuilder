package com.aiapkbuilder.app.util

import org.junit.Assert.*
import org.junit.Test

class ProjectGenerationUtilsTest {

    @Test
    fun `generateProjectId returns non-empty string`() {
        val id = ProjectGenerationUtils.generateProjectId()
        assertTrue(id.isNotBlank())
    }

    @Test
    fun `generateProjectId returns unique values`() {
        val id1 = ProjectGenerationUtils.generateProjectId()
        val id2 = ProjectGenerationUtils.generateProjectId()
        assertNotEquals(id1, id2)
    }

    @Test
    fun `generateBuildJobId returns valid id`() {
        val id = ProjectGenerationUtils.generateBuildJobId()
        assertTrue(id.startsWith("build_"))
    }

    @Test
    fun `isValidPackageName accepts valid names`() {
        assertTrue(ProjectGenerationUtils.isValidPackageName("com.example.app"))
        assertTrue(ProjectGenerationUtils.isValidPackageName("com.mycompany.myapp"))
    }

    @Test
    fun `isValidPackageName rejects invalid names`() {
        assertFalse(ProjectGenerationUtils.isValidPackageName(""))
        assertFalse(ProjectGenerationUtils.isValidPackageName("invalid"))
    }

    @Test
    fun `generateScreenRoute returns valid route`() {
        val route = ProjectGenerationUtils.generateScreenRoute("Home")
        assertEquals("home", route)
    }

    @Test
    fun `generateScreenRoute handles spaces`() {
        val route = ProjectGenerationUtils.generateScreenRoute("User Profile")
        assertEquals("user_profile", route)
    }

    @Test
    fun `appNameToPackageName converts correctly`() {
        val pkg = ProjectGenerationUtils.appNameToPackageName("My Weather App")
        assertEquals("com.example.myweatherapp", pkg)
    }

    @Test
    fun `appNameToPackageName handles special chars`() {
        val pkg = ProjectGenerationUtils.appNameToPackageName("Hello!")
        assertEquals("com.example.hello", pkg)
    }

    @Test
    fun `isValidAppName validates correctly`() {
        assertTrue(ProjectGenerationUtils.isValidAppName("Weather App"))
        assertFalse(ProjectGenerationUtils.isValidAppName(""))
        assertFalse(ProjectGenerationUtils.isValidAppName("1Invalid"))
    }

    @Test
    fun `getScreenType returns correct enum`() {
        assertEquals(ScreenType.LIST, ProjectGenerationUtils.getScreenType("list"))
        assertEquals(ScreenType.DETAIL, ProjectGenerationUtils.getScreenType("detail"))
        assertEquals(ScreenType.FORM, ProjectGenerationUtils.getScreenType("form"))
        assertEquals(ScreenType.CUSTOM, ProjectGenerationUtils.getScreenType("unknown"))
    }

    @Test
    fun `ApkSizeEstimator returns positive values`() {
        val size = ApkSizeEstimator.estimateSize("calculator", 3)
        assertTrue(size > 0)
    }

    @Test
    fun `ApkSizeEstimator handles unknown app type`() {
        val size = ApkSizeEstimator.estimateSize("unknown", 0)
        assertTrue(size == 4000)
    }

    @Test
    fun `BuildTimeEstimator returns positive time`() {
        val time = BuildTimeEstimator.estimateBuildTime("calculator", "github_actions")
        assertTrue(time > 0)
    }

    @Test
    fun `FilePathUtils generates correct paths`() {
        assertTrue(FilePathUtils.getProjectSourcePath("p1").contains("projects/p1"))
        assertTrue(FilePathUtils.getApkPath("p1").contains("app-release.apk"))
    }
}
