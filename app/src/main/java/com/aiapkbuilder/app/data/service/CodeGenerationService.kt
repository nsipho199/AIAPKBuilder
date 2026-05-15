package com.aiapkbuilder.app.data.service

import com.aiapkbuilder.app.data.model.GeneratedProjectPlan
import com.aiapkbuilder.app.data.model.GenerationRequest

/**
 * Interface for code generation service.
 * Handles generation of Android project code from prompts.
 */
interface CodeGenerationService {
    /**
     * Analyzes a user prompt and generates a project plan.
     * @param request The generation request with prompt and options
     * @return GeneratedProjectPlan with app structure and code specs
     */
    suspend fun analyzePromptAndPlan(request: GenerationRequest): Result<GeneratedProjectPlan>

    /**
     * Generates Kotlin code for a Compose screen.
     * @param screenName Name of the screen
     * @param description Screen description/purpose
     * @param uiComponents List of UI components to include
     * @return Generated Kotlin Compose code
     */
    suspend fun generateComposeScreen(
        screenName: String,
        description: String,
        uiComponents: List<String>
    ): Result<String>

    /**
     * Generates ViewModel code for a screen.
     * @param screenName Screen name
     * @param dataModels Data models this ViewModel uses
     * @return Generated ViewModel code
     */
    suspend fun generateViewModelCode(
        screenName: String,
        dataModels: List<String>
    ): Result<String>

    /**
     * Generates Repository code for data access.
     * @param repositoryName Name of the repository
     * @param dataSource Type of data source (api, local, etc.)
     * @return Generated Repository code
     */
    suspend fun generateRepositoryCode(
        repositoryName: String,
        dataSource: String
    ): Result<String>

    /**
     * Generates Navigation setup code.
     * @param screens List of screen routes
     * @return Generated NavHost Compose code
     */
    suspend fun generateNavigationCode(screens: List<String>): Result<String>

    /**
     * Generates Room Database models and DAOs.
     * @param tables List of table specifications
     * @return Generated Database code
     */
    suspend fun generateDatabaseCode(tables: List<String>): Result<String>

    /**
     * Generates build.gradle configuration for the app.
     * @param appType Type of app
     * @param features Features to include
     * @return Generated Gradle config
     */
    suspend fun generateGradleConfig(
        appType: String,
        features: List<String>
    ): Result<String>

    /**
     * Generates AndroidManifest.xml configuration.
     * @param packageName App package name
     * @param appName App display name
     * @param permissions Required permissions
     * @return Generated manifest XML
     */
    suspend fun generateManifest(
        packageName: String,
        appName: String,
        permissions: List<String>
    ): Result<String>
}
