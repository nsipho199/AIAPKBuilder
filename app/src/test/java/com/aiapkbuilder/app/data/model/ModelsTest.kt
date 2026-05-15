package com.aiapkbuilder.app.data.model

import org.junit.Assert.*
import org.junit.Test

class ModelsTest {

    @Test
    fun `AppProject has correct defaults`() {
        val project = AppProject(id = "test1", name = "Test", description = "desc", prompt = "prompt", appType = AppType.CALCULATOR)
        assertEquals(BuildStatus.PENDING, project.buildStatus)
        assertFalse(project.isFavorite)
        assertEquals(0, project.totalBuilds)
        assertEquals(26, project.minSdk)
    }

    @Test
    fun `AppType entries cover all types`() {
        assertEquals(18, AppType.entries.size)
        assertTrue(AppType.entries.all { it.displayName.isNotBlank() })
    }

    @Test
    fun `BuildStatus transitions are valid`() {
        val ordered = listOf(BuildStatus.PENDING, BuildStatus.GENERATING, BuildStatus.BUILDING, BuildStatus.SUCCESS)
        assertEquals("Pending", BuildStatus.PENDING.displayName)
        assertEquals("Ready", BuildStatus.SUCCESS.displayName)
        assertEquals("Failed", BuildStatus.FAILED.displayName)
    }

    @Test
    fun `BuildProvider entries match expected`() {
        assertEquals(6, BuildProvider.entries.size)
        assertTrue(BuildProvider.entries.any { it == BuildProvider.GITHUB_ACTIONS })
        assertTrue(BuildProvider.entries.any { it == BuildProvider.DOCKER })
    }

    @Test
    fun `AIProvider entries cover providers`() {
        assertEquals(5, AIProvider.entries.size)
        assertTrue(AIProvider.entries.any { it == AIProvider.OLLAMA })
        assertEquals("Ollama (Local)", AIProvider.OLLAMA.displayName)
    }

    @Test
    fun `GenerationRequest defaults are correct`() {
        val req = GenerationRequest(prompt = "Build a chat app")
        assertEquals("Build a chat app", req.prompt)
        assertNull(req.appType)
        assertTrue(req.additionalFeatures.isEmpty())
    }

    @Test
    fun `BuildJob defaults are correct`() {
        val job = BuildJob(jobId = "j1", projectId = "p1", provider = BuildProvider.LOCAL, status = BuildStatus.PENDING)
        assertEquals(BuildStatus.PENDING, job.status)
        assertEquals(0, job.progressPercent)
        assertNull(job.completedAt)
    }

    @Test
    fun `AISettings defaults are correct`() {
        val settings = AISettings()
        assertEquals(AIProvider.OPENAI, settings.provider)
        assertEquals("gpt-4o", settings.model)
        assertEquals(4096, settings.maxTokens)
    }

    @Test
    fun `BuildSettings defaults are correct`() {
        val settings = BuildSettings()
        assertEquals(BuildProvider.GITHUB_ACTIONS, settings.defaultProvider)
    }

    @Test
    fun `BuildConfig is Serializable`() {
        val config = BuildConfig(id = "c1", projectId = "p1", provider = BuildProvider.DOCKER, configJson = "{}")
        assertTrue(config is java.io.Serializable)
    }

    @Test
    fun `CodeGenerationCache has 24h expiry`() {
        val cache = CodeGenerationCache(id = "cc1", projectId = "p1", screenName = "Home", generatedCode = "", codeType = "compose")
        val diff = cache.expiresAt - cache.generatedAt
        assertEquals(24 * 60 * 60 * 1000, diff)
    }

    @Test
    fun `ProjectTemplate has all fields`() {
        val tpl = ProjectTemplate(id = "t1", name = "Calculator", appType = AppType.CALCULATOR, description = "A calculator", category = "Utility", difficulty = "beginner", features = listOf("add", "subtract"), screenCount = 1, estimatedBuildTime = 300, baseGradleTemplate = "", screensTemplate = "", isBuiltIn = true)
        assertTrue(tpl.isBuiltIn)
        assertEquals("beginner", tpl.difficulty)
    }

    @Test
    fun `GeneratedProjectPlan has all required screens`() {
        val plan = GeneratedProjectPlan(appName = "MyApp", packageName = "com.myapp", description = "desc", appType = AppType.CUSTOM, screens = listOf(ScreenSpec("Home", "/home", "Home screen", listOf("Text"))), features = listOf("Dark Mode"), dependencies = listOf("compose"), colorPrimary = "#000", colorSecondary = "#fff", minSdkVersion = 26, permissions = listOf("INTERNET"), navigationStructure = "Bottom", databaseTables = listOf("items"), apiEndpoints = emptyList())
        assertEquals(1, plan.screens.size)
        assertEquals("Home", plan.screens[0].name)
    }

    @Test
    fun `StringListConverter roundtrip`() {
        val converter = StringListConverter()
        val original = listOf("a", "b", "c")
        val json = converter.fromStringList(original)
        val restored = converter.toStringList(json)
        assertEquals(original, restored)
    }

    @Test
    fun `MapConverters roundtrip`() {
        val converter = MapConverters()
        val original = mapOf("key1" to "val1", "key2" to "val2")
        val json = converter.fromMap(original)
        val restored = converter.toMap(json)
        assertEquals(original, restored)
    }

    @Test
    fun `StringListConverter handles empty list`() {
        val converter = StringListConverter()
        assertEquals("[]", converter.fromStringList(emptyList()))
        assertTrue(converter.toStringList("[]").isEmpty())
    }

    @Test
    fun `MapConverters handles empty map`() {
        val converter = MapConverters()
        assertEquals("{}", converter.fromMap(emptyMap()))
        assertTrue(converter.toMap("{}").isEmpty())
    }
}
