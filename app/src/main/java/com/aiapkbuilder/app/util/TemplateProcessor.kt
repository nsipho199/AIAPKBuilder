package com.aiapkbuilder.app.util

import com.aiapkbuilder.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes templates with variable substitution.
 * Replaces placeholders in code templates with actual values.
 */
@Singleton
class TemplateProcessor @Inject constructor() {

    /**
     * Process a template string with variable substitution
     */
    fun processTemplate(
        template: String,
        variables: Map<String, String>
    ): String {
        var result = template

        // Replace all variables
        variables.forEach { (key, value) ->
            val placeholder = "{{$key}}" // or ${key}
            result = result.replace(placeholder, value)
        }

        // Also support ${variable} syntax
        variables.forEach { (key, value) ->
            val placeholder = "\${$key}"
            result = result.replace(placeholder, value)
        }

        return result
    }

    /**
     * Process a screen template with screen-specific variables
     */
    fun processScreenTemplate(
        template: String,
        screen: ScreenBlueprint,
        packageName: String
    ): String {
        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "SCREEN_NAME" to screen.name,
            "SCREEN_ROUTE" to screen.route,
            "VIEWMODEL_NAME" to screen.dataBinding.viewModelName,
            "UI_STATE_TYPE" to screen.dataBinding.stateType,
            "UI_EVENT_TYPE" to screen.dataBinding.eventType
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a ViewModel template
     */
    fun processViewModelTemplate(
        template: String,
        viewModel: ViewModelBlueprint,
        packageName: String
    ): String {
        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "VIEWMODEL_NAME" to viewModel.name,
            "UI_STATE_TYPE" to viewModel.stateClass,
            "UI_EVENT_TYPE" to viewModel.eventClass,
            "REPOSITORY_NAME" to viewModel.dependencies.firstOrNull()?.second ?: "Repository"
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a repository template
     */
    fun processRepositoryTemplate(
        template: String,
        repository: RepositoryBlueprint,
        packageName: String
    ): String {
        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "REPOSITORY_NAME" to repository.name,
            "REPOSITORY_INTERFACE" to repository.name.replace("Repository", "RepositoryInterface"),
            "DATA_SOURCE" to repository.dataSource,
            "DAO_NAME" to repository.dependencies.find { it.second.contains("Dao") }?.second ?: "Dao",
            "API_SERVICE_NAME" to repository.dependencies.find { it.second.contains("ApiService") }?.second ?: "ApiService"
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a data model template
     */
    fun processDataModelTemplate(
        template: String,
        dataModel: DataModelSpec,
        packageName: String
    ): String {
        val propertiesString = dataModel.properties.joinToString(",\n    ") { (name, type) ->
            "val $name: $type"
        }

        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "MODEL_NAME" to dataModel.name,
            "MODEL_PROPERTIES" to propertiesString,
            "ENTITY_ANNOTATION" to if (dataModel.isEntity) "@Entity(tableName = \"${dataModel.name.lowercase()}s\")" else ""
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a Gradle template
     */
    fun processGradleTemplate(
        template: String,
        dependencies: List<Dependency>,
        buildConfig: BuildConfigSpec
    ): String {
        val dependenciesString = dependencies.joinToString("\n    ") { dep ->
            "implementation \"${dep.artifact}\""
        }

        val variables = mapOf(
            "DEPENDENCIES" to dependenciesString,
            "APPLICATION_ID" to buildConfig.applicationId,
            "MIN_SDK" to buildConfig.minSdk.toString(),
            "TARGET_SDK" to buildConfig.targetSdk.toString(),
            "VERSION_CODE" to buildConfig.versionCode.toString(),
            "VERSION_NAME" to buildConfig.versionName
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a manifest template
     */
    fun processManifestTemplate(
        template: String,
        packageName: String,
        appName: String,
        permissions: List<String>
    ): String {
        val permissionsString = permissions.joinToString("\n    ") { permission ->
            "<uses-permission android:name=\"$permission\" />"
        }

        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "APP_NAME" to appName,
            "PERMISSIONS" to permissionsString
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a navigation template
     */
    fun processNavigationTemplate(
        template: String,
        screens: List<ScreenBlueprint>,
        packageName: String
    ): String {
        val navItems = screens.joinToString("\n        ") { screen ->
            "composable(\"${screen.route}\") { ${screen.name}() }"
        }

        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "NAV_ITEMS" to navItems,
            "START_DESTINATION" to (screens.firstOrNull()?.route ?: "home")
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a theme template
     */
    fun processThemeTemplate(
        template: String,
        colorScheme: ColorScheme,
        packageName: String
    ): String {
        val variables = mapOf(
            "PACKAGE_NAME" to packageName,
            "PRIMARY_COLOR" to colorScheme.primary,
            "SECONDARY_COLOR" to colorScheme.secondary,
            "TERTIARY_COLOR" to colorScheme.tertiary
        )

        return processTemplate(template, variables)
    }

    /**
     * Process a strings template
     */
    fun processStringsTemplate(
        template: String,
        appName: String,
        features: List<String>
    ): String {
        val featureStrings = features.take(5).mapIndexed { index, feature ->
            "<string name=\"feature_${index + 1}\">$feature</string>"
        }.joinToString("\n    ")

        val variables = mapOf(
            "APP_NAME" to appName,
            "FEATURE_STRINGS" to featureStrings
        )

        return processTemplate(template, variables)
    }

    /**
     * Get built-in templates
     */
    fun getBuiltInTemplates(): Map<String, String> {
        return mapOf(
            "screen_basic" to getBasicScreenTemplate(),
            "viewmodel_basic" to getBasicViewModelTemplate(),
            "repository_basic" to getBasicRepositoryTemplate(),
            "model_entity" to getEntityModelTemplate(),
            "gradle_basic" to getBasicGradleTemplate(),
            "manifest_basic" to getBasicManifestTemplate(),
            "navigation_basic" to getBasicNavigationTemplate(),
            "theme_basic" to getBasicThemeTemplate(),
            "strings_basic" to getBasicStringsTemplate()
        )
    }

    private fun getBasicScreenTemplate(): String {
        return """
package {{PACKAGE_NAME}}.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun {{SCREEN_NAME}}(
    viewModel: {{VIEWMODEL_NAME}} = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("{{SCREEN_NAME}}") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen content goes here
            Text(
                text = "{{SCREEN_NAME}} Content",
                style = MaterialTheme.typography.headlineMedium
            )

            // Add more components based on blueprint
        }
    }
}

@Preview(showBackground = true)
@Composable
fun {{SCREEN_NAME}}Preview() {
    MaterialTheme {
        {{SCREEN_NAME}}()
    }
}
        """.trimIndent()
    }

    private fun getBasicViewModelTemplate(): String {
        return """
package {{PACKAGE_NAME}}.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class {{VIEWMODEL_NAME}} @Inject constructor(
    private val repository: {{REPOSITORY_NAME}}
) : ViewModel() {

    private val _uiState = MutableStateFlow({{UI_STATE_TYPE}}())
    val uiState: StateFlow<{{UI_STATE_TYPE}}> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getData().collect { data ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        data = data,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }

    fun onEvent(event: {{UI_EVENT_TYPE}}) {
        when (event) {
            is {{UI_EVENT_TYPE}}.LoadData -> loadData()
            // Handle other events
        }
    }
}
        """.trimIndent()
    }

    private fun getBasicRepositoryTemplate(): String {
        return """
package {{PACKAGE_NAME}}.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface {{REPOSITORY_INTERFACE}} {
    fun getData(): Flow<List<Any>>
    suspend fun insertData(data: Any)
    suspend fun updateData(data: Any)
    suspend fun deleteData(id: String)
}

@Singleton
class {{REPOSITORY_NAME}} @Inject constructor(
    private val {{DATA_SOURCE}}: Any // Replace with actual dependency
) : {{REPOSITORY_INTERFACE}} {

    override fun getData(): Flow<List<Any>> = flow {
        try {
            // Implement data fetching based on DATA_SOURCE
            emit(emptyList())
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun insertData(data: Any) {
        // Implement insert logic
    }

    override suspend fun updateData(data: Any) {
        // Implement update logic
    }

    override suspend fun deleteData(id: String) {
        // Implement delete logic
    }
}
        """.trimIndent()
    }

    private fun getEntityModelTemplate(): String {
        return """
package {{PACKAGE_NAME}}.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

{{ENTITY_ANNOTATION}}
data class {{MODEL_NAME}}(
    @PrimaryKey
    val id: String,
    {{MODEL_PROPERTIES}}
)
        """.trimIndent()
    }

    private fun getBasicGradleTemplate(): String {
        return """
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'dagger.hilt.android.plugin'
}

android {
    namespace '{{PACKAGE_NAME}}'
    compileSdk 34

    defaultConfig {
        applicationId "{{APPLICATION_ID}}"
        minSdk {{MIN_SDK}}
        targetSdk {{TARGET_SDK}}
        versionCode {{VERSION_CODE}}
        versionName "{{VERSION_NAME}}"

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
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
    implementation 'androidx.activity:activity-compose:1.8.2'
    implementation 'androidx.compose.ui:ui:1.6.0'
    implementation 'androidx.compose.ui:ui-graphics:1.6.0'
    implementation 'androidx.compose.ui:ui-tooling-preview:1.6.0'
    implementation 'androidx.compose.material3:material3:1.1.2'
    implementation 'androidx.navigation:navigation-compose:2.7.5'
    implementation 'com.google.dagger:hilt-android:2.48'
    kapt 'com.google.dagger:hilt-compiler:2.48'
    implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'

    {{DEPENDENCIES}}
}
        """.trimIndent()
    }

    private fun getBasicManifestTemplate(): String {
        return """
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    {{PERMISSIONS}}

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="{{APP_NAME}}"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.{{APP_NAME}}"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.{{APP_NAME}}">
            <intent-filter>
                android:name="android.intent.action.MAIN"
                android:category="android.intent.category.LAUNCHER"
            </intent-filter>
        </activity>
    </application>

</manifest>
        """.trimIndent()
    }

    private fun getBasicNavigationTemplate(): String {
        return """
package {{PACKAGE_NAME}}.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "{{START_DESTINATION}}") {
        {{NAV_ITEMS}}
    }
}
        """.trimIndent()
    }

    private fun getBasicThemeTemplate(): String {
        return """
package {{PACKAGE_NAME}}.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF{{PRIMARY_COLOR}}),
    secondary = Color(0xFF{{SECONDARY_COLOR}}),
    tertiary = Color(0xFF{{TERTIARY_COLOR}})
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

    private fun getBasicStringsTemplate(): String {
        return """
<resources>
    <string name="app_name">{{APP_NAME}}</string>
    <string name="home">Home</string>
    <string name="settings">Settings</string>
    <string name="loading">Loading...</string>
    <string name="error">Error</string>
    <string name="retry">Retry</string>
    {{FEATURE_STRINGS}}
</resources>
        """.trimIndent()
    }
}