package com.aiapkbuilder.app.data.model

/**
 * Comprehensive blueprint for an entire Android project.
 * Contains all specifications needed to generate the complete app.
 */
data class ProjectBlueprint(
    val appName: String,
    val packageName: String,
    val screens: List<ScreenBlueprint>,
    val viewModels: List<ViewModelBlueprint>,
    val repositories: List<RepositoryBlueprint>,
    val dataModels: List<DataModelSpec>,
    val dependencies: List<Dependency>,
    val permissions: List<String>,
    val colorScheme: ColorScheme,
    val databaseSchema: DatabaseSchema,
    val apiClients: List<ApiClientSpec>,
    val buildConfig: BuildConfigSpec
)

/**
 * Detailed specification for a single screen.
 */
data class ScreenBlueprint(
    val name: String,
    val route: String,
    val type: ScreenType,
    val components: List<ComponentSpec>,
    val dataBinding: DataBinding,
    val navigation: List<NavigationEdge>
)

/**
 * Specification for a UI component within a screen.
 */
data class ComponentSpec(
    val name: String,
    val type: ComponentType,
    val properties: Map<String, String>,
    val eventHandlers: List<EventHandler>
)

/**
 * Type of UI component.
 */
enum class ComponentType {
    BUTTON, TEXT, LIST, CARD, BAR, FORM, IMAGE, CUSTOM
}

/**
 * Event handler for a component.
 */
data class EventHandler(
    val eventName: String,
    val handlerCode: String
)

/**
 * Data binding specification for a screen.
 */
data class DataBinding(
    val viewModelName: String,
    val stateType: String,
    val eventType: String
)

/**
 * Navigation edge between screens.
 */
data class NavigationEdge(
    val fromScreen: String,
    val toScreen: String,
    val trigger: String,
    val parameters: List<String> = emptyList()
)

/**
 * Specification for a ViewModel.
 */
data class ViewModelBlueprint(
    val name: String,
    val stateClass: String,
    val eventClass: String,
    val dependencies: List<Pair<String, String>>, // name -> type
    val functions: List<String>
)

/**
 * Specification for a Repository.
 */
data class RepositoryBlueprint(
    val name: String,
    val dataSource: String, // "api", "local", "both"
    val functions: List<String>,
    val dependencies: List<Pair<String, String>> // name -> type
)

/**
 * Specification for a data model.
 */
data class DataModelSpec(
    val name: String,
    val properties: List<Pair<String, String>>, // name -> type
    val isEntity: Boolean // true if it should be a Room entity
)

/**
 * Dependency specification.
 */
data class Dependency(
    val artifact: String,
    val description: String
)

/**
 * Color scheme for the app.
 */
data class ColorScheme(
    val primary: String,
    val secondary: String,
    val tertiary: String
)

/**
 * Database schema specification.
 */
data class DatabaseSchema(
    val tables: List<DatabaseTable>
)

/**
 * Database table specification.
 */
data class DatabaseTable(
    val name: String,
    val columns: List<DatabaseColumn>
)

/**
 * Database column specification.
 */
data class DatabaseColumn(
    val name: String,
    val type: String // SQL type: TEXT, INTEGER, REAL
)

/**
 * API client specification.
 */
data class ApiClientSpec(
    val name: String,
    val baseUrl: String,
    val endpoints: List<String>
)

/**
 * Build configuration specification.
 */
data class BuildConfigSpec(
    val minSdk: Int,
    val targetSdk: Int,
    val versionCode: Int,
    val versionName: String,
    val applicationId: String,
    val buildTypes: List<String>,
    val features: List<String>
)