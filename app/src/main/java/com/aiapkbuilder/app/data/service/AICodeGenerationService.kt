package com.aiapkbuilder.app.data.service

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.util.CodeGenerator
import com.aiapkbuilder.app.util.PromptAnalyzer
import com.aiapkbuilder.app.util.safeExecute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered implementation of CodeGenerationService.
 * Uses AI providers to analyze prompts and generate code.
 */
@Singleton
class AICodeGenerationService @Inject constructor(
    private val aiProviderManager: AIProviderManager,
    private val projectPlanner: ProjectPlannerService,
    private val codeGenerator: CodeGenerator,
    private val promptAnalyzer: PromptAnalyzer
) : CodeGenerationService {

    override suspend fun analyzePromptAndPlan(request: GenerationRequest): Result<GeneratedProjectPlan> = safeExecute {
        // Step 1: Analyze the prompt using NLP
        val extractedRequirements = promptAnalyzer.analyzePrompt(request.prompt)
            .getOrElse { throw it }

        // Step 2: Create AI prompt for detailed planning
        val planningPrompt = createPlanningPrompt(extractedRequirements)

        // Step 3: Get AI response
        val aiResponse = aiProviderManager.executePrompt(
            prompt = planningPrompt,
            systemMessage = createSystemMessage()
        ).getOrElse { throw it }

        // Step 4: Parse AI response into project plan
        val projectPlan = parseAIResponse(aiResponse, extractedRequirements)

        projectPlan
    }

    override suspend fun generateComposeScreen(
        screenName: String,
        description: String,
        uiComponents: List<String>
    ): Result<String> {
        val prompt = """
Generate a Jetpack Compose screen for Android with the following specifications:

Screen Name: $screenName
Description: $description
UI Components: ${uiComponents.joinToString(", ")}

Requirements:
- Use Material3 components
- Follow Compose best practices
- Include proper imports
- Add preview function
- Handle state management
- Include navigation if needed

Generate the complete Kotlin code for this screen.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer specializing in Jetpack Compose. Generate clean, production-ready code."
        )
    }

    override suspend fun generateViewModelCode(
        screenName: String,
        dataModels: List<String>
    ): Result<String> {
        val prompt = """
Generate a ViewModel for an Android screen with the following specifications:

Screen Name: $screenName
Data Models: ${dataModels.joinToString(", ")}

Requirements:
- Use Hilt for dependency injection
- Implement StateFlow for UI state
- Handle events with sealed classes
- Include error handling
- Use coroutines for async operations
- Follow MVVM pattern

Generate the complete Kotlin ViewModel code.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer. Generate clean, testable ViewModel code following Android architecture guidelines."
        )
    }

    override suspend fun generateRepositoryCode(
        repositoryName: String,
        dataSource: String
    ): Result<String> {
        val prompt = """
Generate a Repository class for Android with the following specifications:

Repository Name: $repositoryName
Data Source: $dataSource

Requirements:
- Implement repository pattern
- Use Flow for reactive data
- Handle both local and remote data sources
- Include error handling
- Use dependency injection
- Follow clean architecture principles

Generate the complete Kotlin Repository code.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer. Generate repository code that properly handles data sources and follows repository pattern."
        )
    }

    override suspend fun generateNavigationCode(screens: List<String>): Result<String> {
        val prompt = """
Generate Jetpack Compose Navigation code for the following screens:

Screens: ${screens.joinToString(", ")}

Requirements:
- Use Navigation Compose
- Create NavHost with proper routes
- Include navigation actions
- Handle back navigation
- Use type-safe navigation
- Include proper imports

Generate the complete navigation setup code.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer. Generate navigation code that follows Jetpack Compose Navigation best practices."
        )
    }

    override suspend fun generateDatabaseCode(tables: List<String>): Result<String> {
        val prompt = """
Generate Room database code for Android with the following tables:

Tables: ${tables.joinToString(", ")}

Requirements:
- Create Room entities
- Generate DAO interfaces
- Create Database class
- Include proper annotations
- Handle relationships
- Use type converters if needed
- Include migration support

Generate the complete Room database setup code.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer. Generate Room database code that follows Android database best practices."
        )
    }

    override suspend fun generateGradleConfig(
        appType: String,
        features: List<String>
    ): Result<String> {
        val prompt = """
Generate build.gradle.kts configuration for an Android app with the following specifications:

App Type: $appType
Features: ${features.joinToString(", ")}

Requirements:
- Include necessary plugins
- Configure Android block properly
- Add required dependencies
- Include build types
- Configure compose options
- Add proguard rules

Generate the complete build.gradle.kts file.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer. Generate proper Gradle configuration for Android projects."
        )
    }

    override suspend fun generateManifest(
        packageName: String,
        appName: String,
        permissions: List<String>
    ): Result<String> {
        val prompt = """
Generate AndroidManifest.xml for an Android app with the following specifications:

Package Name: $packageName
App Name: $appName
Permissions: ${permissions.joinToString(", ")}

Requirements:
- Include proper package declaration
- Add all required permissions
- Configure main activity
- Include intent filters
- Add proper attributes
- Include backup rules

Generate the complete AndroidManifest.xml file.
        """.trimIndent()

        return aiProviderManager.executePrompt(
            prompt = prompt,
            systemMessage = "You are an expert Android developer. Generate proper Android manifest files."
        )
    }

    private fun createSystemMessage(): String {
        return """
You are an expert Android app architect and developer. Your task is to analyze user requirements and create detailed project specifications for Android applications using modern Android development practices.

You should:
- Understand the user's intent from natural language descriptions
- Identify appropriate app architecture and patterns
- Specify screens, data models, and navigation flows
- Choose appropriate technologies and libraries
- Consider user experience and best practices
- Provide detailed, actionable specifications

Always respond with structured, parseable output that can be used to generate actual code.
        """.trimIndent()
    }

    private fun createPlanningPrompt(requirements: ExtractedRequirements): String {
        return """
Based on the following user requirements, create a detailed Android app specification:

User Request: "${requirements.appName}"
App Type: ${requirements.appType}
Target Audience: ${requirements.targetAudience}
Complexity: ${requirements.complexity}
Features: ${requirements.primaryFeatures.joinToString(", ")}
Additional Features: ${requirements.secondaryFeatures.joinToString(", ")}

Please provide a detailed specification including:

1. APP OVERVIEW
   - App name and description
   - Package name
   - Target platforms

2. SCREENS & NAVIGATION
   - List all screens with descriptions
   - Navigation flow between screens
   - Screen types (list, detail, form, etc.)

3. DATA MODELS
   - Database entities needed
   - API models if applicable
   - Relationships between models

4. FEATURES & FUNCTIONALITY
   - Core features implementation
   - UI components needed
   - Business logic requirements

5. TECHNICAL SPECIFICATIONS
   - Dependencies and libraries
   - Permissions required
   - Build configuration
   - Architecture patterns

Provide the response in a structured format that can be parsed programmatically.
        """.trimIndent()
    }

    private fun parseAIResponse(
        aiResponse: String,
        requirements: ExtractedRequirements
    ): GeneratedProjectPlan {
        // For now, create a structured plan based on the requirements
        // In a full implementation, this would parse the AI response JSON

        return GeneratedProjectPlan(
            appName = requirements.appName,
            packageName = "com.generated.${requirements.appName.lowercase().replace(Regex("[^a-z0-9]"), "")}",
            description = requirements.appName,
            appType = requirements.appType,
            screens = createScreenSpecs(requirements),
            features = requirements.primaryFeatures + requirements.secondaryFeatures,
            dependencies = determineDependencies(requirements),
            colorPrimary = "#6200EA",
            colorSecondary = "#03DAC6",
            minSdkVersion = 26,
            permissions = determinePermissions(requirements),
            navigationStructure = "Bottom Navigation",
            databaseTables = determineTables(requirements),
            apiEndpoints = determineApiEndpoints(requirements)
        )
    }

    private fun createScreenSpecs(requirements: ExtractedRequirements): List<ScreenSpec> {
        val screens = mutableListOf<ScreenSpec>()

        when (requirements.appType) {
            AppType.WEATHER -> {
                screens.add(ScreenSpec("HomeScreen", "/home", "Current weather display", listOf("TopAppBar", "WeatherCard", "ForecastList")))
                screens.add(ScreenSpec("SearchScreen", "/search", "Location search", listOf("SearchBar", "LocationList")))
            }
            AppType.NOTES -> {
                screens.add(ScreenSpec("NotesListScreen", "/notes", "List of notes", listOf("NotesList", "AddButton")))
                screens.add(ScreenSpec("NoteDetailScreen", "/note/{id}", "Note details", listOf("NoteContent", "EditButton")))
            }
            AppType.CHAT -> {
                screens.add(ScreenSpec("ConversationsScreen", "/conversations", "Chat list", listOf("ConversationsList")))
                screens.add(ScreenSpec("ChatScreen", "/chat/{id}", "Chat conversation", listOf("MessagesList", "MessageInput")))
            }
            else -> {
                screens.add(ScreenSpec("HomeScreen", "/home", "Main screen", listOf("ContentArea")))
                screens.add(ScreenSpec("DetailScreen", "/detail", "Detail view", listOf("DetailContent")))
            }
        }

        return screens
    }

    private fun determineDependencies(requirements: ExtractedRequirements): List<String> {
        val deps = mutableListOf("androidx-core-ktx", "androidx-lifecycle-viewmodel-compose", "androidx-activity-compose")

        if (requirements.databaseNeed) {
            deps.addAll(listOf("androidx-room-runtime", "androidx-room-ktx"))
        }

        if (requirements.apiIntegration) {
            deps.addAll(listOf("com.squareup.retrofit2:retrofit", "com.squareup.retrofit2:converter-gson"))
        }

        return deps
    }

    private fun determinePermissions(requirements: ExtractedRequirements): List<String> {
        val permissions = mutableListOf<String>()

        if (requirements.apiIntegration) {
            permissions.add("android.permission.INTERNET")
        }

        if (requirements.primaryFeatures.any { it.contains("location") }) {
            permissions.add("android.permission.ACCESS_FINE_LOCATION")
        }

        return permissions
    }

    private fun determineTables(requirements: ExtractedRequirements): List<String> {
        return when (requirements.appType) {
            AppType.NOTES -> listOf("notes", "categories")
            AppType.CHAT -> listOf("conversations", "messages", "users")
            AppType.ECOMMERCE -> listOf("products", "orders", "users")
            else -> listOf("items")
        }
    }

    private fun determineApiEndpoints(requirements: ExtractedRequirements): List<String> {
        return when (requirements.appType) {
            AppType.WEATHER -> listOf("/api/weather/current", "/api/weather/forecast")
            AppType.ECOMMERCE -> listOf("/api/products", "/api/orders", "/api/users")
            else -> emptyList()
        }
    }
}