package com.aiapkbuilder.app.util

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.data.service.CodeGenerationService
import com.aiapkbuilder.app.util.generators.*
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main orchestrator for code generation.
 * Implements the CodeGenerationService interface and coordinates all generators.
 */
@Singleton
class CodeGenerator @Inject constructor(
    private val gson: Gson
) : CodeGenerationService {

    private val screenGenerator = ComposeScreenGenerator()
    private val viewModelGenerator = ViewModelGenerator()
    private val repositoryGenerator = RepositoryGenerator()
    private val databaseGenerator = DatabaseGenerator()

    override suspend fun analyzePromptAndPlan(request: GenerationRequest): Result<GeneratedProjectPlan> {
        // This would normally use AI, but for now we'll create a fallback plan
        return createFallbackPlan(request)
    }

    override suspend fun generateComposeScreen(
        screenName: String,
        description: String,
        uiComponents: List<String>
    ): Result<String> {
        // Create a basic screen blueprint
        val blueprint = ScreenBlueprint(
            name = screenName,
            route = screenName.lowercase(),
            type = ScreenType.DETAIL,
            components = uiComponents.map { component ->
                ComponentSpec(
                    name = component,
                    type = ComponentType.CUSTOM,
                    properties = emptyMap(),
                    eventHandlers = emptyList()
                )
            },
            dataBinding = DataBinding(
                viewModelName = "${screenName}ViewModel",
                stateType = "UiState",
                eventType = "UiEvent"
            ),
            navigation = emptyList()
        )

        return screenGenerator.generateScreen(
            blueprint = blueprint,
            packageName = "com.generated.app",
            viewModelName = "${screenName}ViewModel",
            colorScheme = ColorScheme("#6200EA", "#03DAC6", "#FF5722")
        )
    }

    override suspend fun generateViewModelCode(
        screenName: String,
        dataModels: List<String>
    ): Result<String> {
        val blueprint = ViewModelBlueprint(
            name = "${screenName}ViewModel",
            stateClass = "UiState",
            eventClass = "UiEvent",
            dependencies = listOf("repository" to "${screenName}Repository"),
            functions = listOf(
                "loadData()",
                "handleEvent(event: UiEvent)",
                "updateState(newState: UiState)"
            )
        )

        return viewModelGenerator.generateViewModel(
            blueprint = blueprint,
            packageName = "com.generated.app",
            repositoryName = "${screenName}Repository"
        )
    }

    override suspend fun generateRepositoryCode(
        repositoryName: String,
        dataSource: String
    ): Result<String> {
        val blueprint = RepositoryBlueprint(
            name = repositoryName,
            dataSource = dataSource,
            functions = listOf(
                "getData(): Flow<List<Any>>",
                "getDataById(id: String): Flow<Any?>",
                "insertData(data: Any)",
                "updateData(data: Any)",
                "deleteData(id: String)"
            ),
            dependencies = when (dataSource) {
                "api" -> listOf("apiService" to "${repositoryName.replace("Repository", "")}ApiService")
                "local" -> listOf("dao" to "${repositoryName.replace("Repository", "")}Dao")
                else -> emptyList()
            }
        )

        return repositoryGenerator.generateRepository(
            blueprint = blueprint,
            packageName = "com.generated.app",
            dataModelName = "Any"
        )
    }

    override suspend fun generateNavigationCode(screens: List<String>): Result<String> {
        val navGraph = generateNavGraph(screens)
        return Result.success(navGraph)
    }

    override suspend fun generateDatabaseCode(tables: List<String>): Result<String> {
        // Generate basic database class
        return databaseGenerator.generateDatabase(
            entities = tables,
            packageName = "com.generated.app",
            databaseName = "app_database"
        )
    }

    override suspend fun generateGradleConfig(
        appType: String,
        features: List<String>
    ): Result<String> {
        val gradleConfig = """
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

android {
    namespace 'com.generated.app'
    compileSdk 34

    defaultConfig {
        applicationId "com.generated.app"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.8'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.compose.ui:ui:1.6.0'
    implementation 'androidx.compose.ui:ui-graphics:1.6.0'
    implementation 'androidx.compose.ui:ui-tooling-preview:1.6.0'
    implementation 'androidx.compose.material3:material3:1.1.2'
    implementation 'androidx.navigation:navigation-compose:2.7.5'
    implementation 'com.google.dagger:hilt-android:2.48'
    kapt 'com.google.dagger:hilt-compiler:2.48'
    implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'
}
        """.trimIndent()

        return Result.success(gradleConfig)
    }

    override suspend fun generateManifest(
        packageName: String,
        appName: String,
        permissions: List<String>
    ): Result<String> {
        val permissionsXml = permissions.joinToString("\n    ") { "<uses-permission android:name=\"$it\" />" }

        val manifest = """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    $permissionsXml

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="$appName"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.App"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.App">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
        """.trimIndent()

        return Result.success(manifest)
    }

    /**
     * Generates a complete project from a blueprint.
     */
    suspend fun generateProject(
        blueprint: ProjectBlueprint,
        onProgress: (String) -> Unit = {}
    ): Result<GeneratedProjectFiles> = coroutineScope {
        return@coroutineScope try {
            onProgress("Starting code generation...")

            // Generate screens in parallel
            val screenTasks = blueprint.screens.map { screen ->
                async {
                    onProgress("Generating screen: ${screen.name}")
                    screenGenerator.generateScreen(
                        blueprint = screen,
                        packageName = blueprint.packageName,
                        viewModelName = screen.dataBinding.viewModelName,
                        colorScheme = blueprint.colorScheme
                    )
                }
            }

            // Generate ViewModels in parallel
            val viewModelTasks = blueprint.viewModels.map { vm ->
                async {
                    onProgress("Generating ViewModel: ${vm.name}")
                    viewModelGenerator.generateViewModel(
                        blueprint = vm,
                        packageName = blueprint.packageName,
                        repositoryName = vm.dependencies.firstOrNull()?.second
                    )
                }
            }

            // Generate repositories in parallel
            val repositoryTasks = blueprint.repositories.map { repo ->
                async {
                    onProgress("Generating repository: ${repo.name}")
                    repositoryGenerator.generateRepository(
                        blueprint = repo,
                        packageName = blueprint.packageName,
                        dataModelName = "Any" // TODO: Use actual model names
                    )
                }
            }

            // Generate data models
            val dataModelTasks = blueprint.dataModels.map { model ->
                async {
                    onProgress("Generating data model: ${model.name}")
                    databaseGenerator.generateDataModel(
                        spec = model,
                        packageName = blueprint.packageName
                    )
                }
            }

            // Wait for all generations to complete
            val screens = screenTasks.map { it.await().getOrThrow() }
            val viewModels = viewModelTasks.map { it.await().getOrThrow() }
            val repositories = repositoryTasks.map { it.await().getOrThrow() }
            val dataModels = dataModelTasks.map { it.await().getOrThrow() }

            // Generate other files
            onProgress("Generating navigation...")
            val navigation = generateNavigationCode(blueprint.screens.map { it.name }).getOrThrow()

            onProgress("Generating database...")
            val database = generateDatabaseCode(blueprint.dataModels.map { it.name }).getOrThrow()

            onProgress("Generating Gradle config...")
            val gradle = generateGradleConfig(blueprint.appName, blueprint.features).getOrThrow()

            onProgress("Generating manifest...")
            val manifest = generateManifest(blueprint.packageName, blueprint.appName, blueprint.permissions).getOrThrow()

            // Create theme
            val theme = generateTheme(blueprint.colorScheme, blueprint.packageName)

            // Create strings
            val strings = generateStrings(blueprint.appName)

            val files = GeneratedProjectFiles(
                projectId = "generated_${System.currentTimeMillis()}",
                buildGradle = gradle,
                androidManifest = manifest,
                navigationCode = navigation,
                screens = blueprint.screens.zip(screens).toMap().mapKeys { it.key.name },
                viewModels = blueprint.viewModels.zip(viewModels).toMap().mapKeys { it.key.name },
                repositories = blueprint.repositories.zip(repositories).toMap().mapKeys { it.key.name },
                dataModels = dataModels.joinToString("\n\n"),
                database = database,
                daoClasses = "", // TODO: Generate DAOs
                theme = theme,
                strings = strings
            )

            onProgress("Code generation complete!")
            Result.success(files)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createFallbackPlan(request: GenerationRequest): Result<GeneratedProjectPlan> {
        return Result.success(
            GeneratedProjectPlan(
                appName = request.prompt.take(20).replace(Regex("[^A-Za-z0-9]"), ""),
                packageName = "com.generated.app",
                description = request.prompt,
                appType = request.appType ?: AppType.CUSTOM,
                screens = listOf(
                    ScreenSpec("Home", "/home", "Main screen", listOf("TopAppBar", "LazyColumn")),
                    ScreenSpec("Detail", "/detail", "Detail view", listOf("Card", "Button"))
                ),
                features = request.additionalFeatures,
                dependencies = listOf("compose", "hilt", "room"),
                colorPrimary = "#6200EA",
                colorSecondary = "#03DAC6",
                minSdkVersion = 26,
                permissions = listOf("INTERNET"),
                navigationStructure = "Bottom Navigation",
                databaseTables = listOf("items"),
                apiEndpoints = emptyList()
            )
        )
    }

    private fun generateNavGraph(screens: List<String>): String {
        val navItems = screens.joinToString("\n        ") { screen ->
            "composable(\"${screen.lowercase()}\") { ${screen}Screen() }"
        }

        return """
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        $navItems
    }
}
        """.trimIndent()
    }

    private fun generateTheme(colorScheme: ColorScheme, packageName: String): String {
        return """
package $packageName.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF${colorScheme.primary.removePrefix("#")}),
    secondary = Color(0xFF${colorScheme.secondary.removePrefix("#")}),
    tertiary = Color(0xFF${colorScheme.tertiary.removePrefix("#")})
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
        """.trimIndent()
    }

    private fun generateStrings(appName: String): String {
        return """
<resources>
    <string name="app_name">$appName</string>
    <string name="home">Home</string>
    <string name="settings">Settings</string>
    <string name="loading">Loading...</string>
    <string name="error">Error</string>
    <string name="retry">Retry</string>
</resources>
        """.trimIndent()
    }
}