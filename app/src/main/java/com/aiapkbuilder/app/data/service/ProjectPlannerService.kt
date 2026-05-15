package com.aiapkbuilder.app.data.service

import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.util.ExtractedRequirements
import com.aiapkbuilder.app.util.safeExecute
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plans the complete structure of an Android app based on extracted requirements.
 * Creates detailed blueprints for screens, ViewModels, repositories, and data models.
 */
@Singleton
class ProjectPlannerService @Inject constructor() {

    /**
     * Creates a comprehensive project blueprint from extracted requirements.
     */
    fun createBlueprint(requirements: ExtractedRequirements): Result<ProjectBlueprint> = safeExecute {
        val screens = planScreens(requirements)
        val viewModels = planViewModels(screens)
        val repositories = planRepositories(requirements, screens)
        val dataModels = planDataModels(requirements, screens)
        val dependencies = planDependencies(requirements)
        val permissions = planPermissions(requirements)
        val colorScheme = planColorScheme(requirements)
        val databaseSchema = planDatabaseSchema(requirements, dataModels)
        val apiClients = planApiClients(requirements)
        val buildConfig = planBuildConfig(requirements)

        ProjectBlueprint(
            appName = requirements.appName,
            packageName = generatePackageName(requirements.appName),
            screens = screens,
            viewModels = viewModels,
            repositories = repositories,
            dataModels = dataModels,
            dependencies = dependencies,
            permissions = permissions,
            colorScheme = colorScheme,
            databaseSchema = databaseSchema,
            apiClients = apiClients,
            buildConfig = buildConfig
        )
    }

    private fun planScreens(requirements: ExtractedRequirements): List<ScreenBlueprint> {
        val screens = mutableListOf<ScreenBlueprint>()

        // Add core screens based on app type
        when (requirements.appType) {
            AppType.WEATHER -> {
                screens.add(createScreen("HomeScreen", "Current weather display", listOf(
                    "TopAppBar", "CurrentWeatherCard", "HourlyForecast", "DailyForecast"
                ), ScreenType.DETAIL))

                screens.add(createScreen("SearchScreen", "Location search", listOf(
                    "SearchBar", "LocationList", "RecentSearches"
                ), ScreenType.LIST))

                screens.add(createScreen("SettingsScreen", "App settings", listOf(
                    "SettingsList", "UnitToggle", "NotificationToggle"
                ), ScreenType.SETTINGS))
            }

            AppType.NOTES -> {
                screens.add(createScreen("NotesListScreen", "List of all notes", listOf(
                    "TopAppBar", "NotesList", "FloatingActionButton", "SearchBar"
                ), ScreenType.LIST))

                screens.add(createScreen("NoteDetailScreen", "View/edit note", listOf(
                    "TopAppBar", "NoteContent", "SaveButton", "DeleteButton"
                ), ScreenType.DETAIL))

                screens.add(createScreen("CategoriesScreen", "Note categories", listOf(
                    "CategoryList", "AddCategoryButton"
                ), ScreenType.LIST))
            }

            AppType.CHAT -> {
                screens.add(createScreen("ConversationsScreen", "Chat list", listOf(
                    "ConversationsList", "SearchBar", "NewChatButton"
                ), ScreenType.LIST))

                screens.add(createScreen("ChatScreen", "Chat conversation", listOf(
                    "MessagesList", "MessageInput", "SendButton", "TypingIndicator"
                ), ScreenType.DETAIL))

                screens.add(createScreen("ProfileScreen", "User profile", listOf(
                    "ProfileHeader", "SettingsList"
                ), ScreenType.DETAIL))
            }

            AppType.ECOMMERCE -> {
                screens.add(createScreen("ProductListScreen", "Product catalog", listOf(
                    "TopAppBar", "ProductGrid", "CategoryFilter", "SearchBar"
                ), ScreenType.LIST))

                screens.add(createScreen("ProductDetailScreen", "Product details", listOf(
                    "ProductImages", "ProductInfo", "AddToCartButton", "ReviewsSection"
                ), ScreenType.DETAIL))

                screens.add(createScreen("CartScreen", "Shopping cart", listOf(
                    "CartItems", "PriceSummary", "CheckoutButton"
                ), ScreenType.LIST))

                screens.add(createScreen("CheckoutScreen", "Payment process", listOf(
                    "AddressForm", "PaymentForm", "ConfirmButton"
                ), ScreenType.FORM))

                screens.add(createScreen("ProfileScreen", "User account", listOf(
                    "OrderHistory", "AccountSettings"
                ), ScreenType.DETAIL))
            }

            AppType.FITNESS -> {
                screens.add(createScreen("DashboardScreen", "Fitness overview", listOf(
                    "StatsCards", "RecentWorkouts", "GoalsProgress"
                ), ScreenType.DASHBOARD))

                screens.add(createScreen("WorkoutListScreen", "Workout history", listOf(
                    "WorkoutList", "AddWorkoutButton", "FilterChips"
                ), ScreenType.LIST))

                screens.add(createScreen("WorkoutDetailScreen", "Workout details", listOf(
                    "ExerciseList", "Timer", "CompleteButton"
                ), ScreenType.DETAIL))

                screens.add(createScreen("GoalsScreen", "Fitness goals", listOf(
                    "GoalsList", "AddGoalButton", "ProgressCharts"
                ), ScreenType.LIST))
            }

            else -> {
                // Generic app structure
                screens.add(createScreen("HomeScreen", "Main screen", listOf(
                    "TopAppBar", "ContentArea", "ActionButtons"
                ), ScreenType.DETAIL))

                screens.add(createScreen("ListScreen", "Data list", listOf(
                    "ItemsList", "SearchBar", "AddButton"
                ), ScreenType.LIST))

                screens.add(createScreen("DetailScreen", "Item details", listOf(
                    "ItemContent", "EditButton", "DeleteButton"
                ), ScreenType.DETAIL))
            }
        }

        // Add settings screen if needed
        if (requirements.primaryFeatures.any { it.contains("settings") } ||
            requirements.secondaryFeatures.any { it.contains("settings") }) {
            screens.add(createScreen("SettingsScreen", "App settings", listOf(
                "SettingsList", "ThemeToggle", "NotificationToggle"
            ), ScreenType.SETTINGS))
        }

        return screens
    }

    private fun createScreen(
        name: String,
        description: String,
        components: List<String>,
        type: ScreenType
    ): ScreenBlueprint {
        val route = name.replace("Screen", "").lowercase()
        val uiComponents = components.map { component ->
            ComponentSpec(
                name = component,
                type = determineComponentType(component),
                properties = getComponentProperties(component),
                eventHandlers = getComponentEvents(component)
            )
        }

        return ScreenBlueprint(
            name = name,
            route = route,
            type = type,
            components = uiComponents,
            dataBinding = DataBinding(
                viewModelName = "${name.replace("Screen", "")}ViewModel",
                stateType = "UiState",
                eventType = "UiEvent"
            ),
            navigation = emptyList() // Will be filled by navigation planner
        )
    }

    private fun determineComponentType(componentName: String): ComponentType {
        return when {
            componentName.contains("Button") -> ComponentType.BUTTON
            componentName.contains("Text") || componentName.contains("Label") -> ComponentType.TEXT
            componentName.contains("List") || componentName.contains("Grid") -> ComponentType.LIST
            componentName.contains("Card") -> ComponentType.CARD
            componentName.contains("Bar") -> ComponentType.BAR
            componentName.contains("Form") -> ComponentType.FORM
            componentName.contains("Image") -> ComponentType.IMAGE
            else -> ComponentType.CUSTOM
        }
    }

    private fun getComponentProperties(componentName: String): Map<String, String> {
        return when {
            componentName.contains("Button") -> mapOf(
                "text" to "Button Text",
                "enabled" to "true",
                "modifier" to "Modifier.fillMaxWidth()"
            )
            componentName.contains("Text") -> mapOf(
                "text" to "Sample Text",
                "style" to "MaterialTheme.typography.bodyLarge"
            )
            componentName.contains("List") -> mapOf(
                "items" to "emptyList()",
                "modifier" to "Modifier.fillMaxSize()"
            )
            else -> emptyMap()
        }
    }

    private fun getComponentEvents(componentName: String): List<EventHandler> {
        return when {
            componentName.contains("Button") -> listOf(
                EventHandler("onClick", "handleButtonClick()")
            )
            componentName.contains("List") -> listOf(
                EventHandler("onItemClick", "handleItemClick(item)")
            )
            else -> emptyList()
        }
    }

    private fun planViewModels(screens: List<ScreenBlueprint>): List<ViewModelBlueprint> {
        return screens.map { screen ->
            ViewModelBlueprint(
                name = screen.dataBinding.viewModelName,
                stateClass = screen.dataBinding.stateType,
                eventClass = screen.dataBinding.eventType,
                dependencies = listOf(
                    "repository" to "${screen.name.replace("Screen", "")}Repository"
                ),
                functions = listOf(
                    "loadData()",
                    "handleEvent(event: ${screen.dataBinding.eventType})",
                    "updateState(newState: ${screen.dataBinding.stateType})"
                )
            )
        }
    }

    private fun planRepositories(
        requirements: ExtractedRequirements,
        screens: List<ScreenBlueprint>
    ): List<RepositoryBlueprint> {
        val repositories = mutableListOf<RepositoryBlueprint>()

        screens.forEach { screen ->
            val repoName = "${screen.name.replace("Screen", "")}Repository"
            repositories.add(RepositoryBlueprint(
                name = repoName,
                dataSource = if (requirements.apiIntegration) "api" else "local",
                functions = listOf(
                    "getData(): Flow<List<Data>>",
                    "getDataById(id: String): Flow<Data?>",
                    "insertData(data: Data)",
                    "updateData(data: Data)",
                    "deleteData(id: String)"
                ),
                dependencies = if (requirements.apiIntegration) {
                    listOf("apiService" to "${repoName.replace("Repository", "")}ApiService")
                } else {
                    listOf("dao" to "${repoName.replace("Repository", "")}Dao")
                }
            ))
        }

        return repositories.distinctBy { it.name }
    }

    private fun planDataModels(
        requirements: ExtractedRequirements,
        screens: List<ScreenBlueprint>
    ): List<DataModelSpec> {
        val models = mutableListOf<DataModelSpec>()

        // Add models based on app type
        when (requirements.appType) {
            AppType.WEATHER -> {
                models.add(DataModelSpec(
                    name = "Weather",
                    properties = listOf(
                        "temperature" to "Double",
                        "humidity" to "Int",
                        "windSpeed" to "Double",
                        "condition" to "String",
                        "location" to "String"
                    ),
                    isEntity = true
                ))
                models.add(DataModelSpec(
                    name = "Forecast",
                    properties = listOf(
                        "date" to "String",
                        "highTemp" to "Double",
                        "lowTemp" to "Double",
                        "condition" to "String"
                    ),
                    isEntity = false
                ))
            }

            AppType.NOTES -> {
                models.add(DataModelSpec(
                    name = "Note",
                    properties = listOf(
                        "id" to "String",
                        "title" to "String",
                        "content" to "String",
                        "createdAt" to "Long",
                        "updatedAt" to "Long",
                        "category" to "String"
                    ),
                    isEntity = true
                ))
            }

            AppType.CHAT -> {
                models.add(DataModelSpec(
                    name = "Message",
                    properties = listOf(
                        "id" to "String",
                        "senderId" to "String",
                        "content" to "String",
                        "timestamp" to "Long",
                        "isRead" to "Boolean"
                    ),
                    isEntity = true
                ))
                models.add(DataModelSpec(
                    name = "Conversation",
                    properties = listOf(
                        "id" to "String",
                        "participants" to "List<String>",
                        "lastMessage" to "String",
                        "lastMessageTime" to "Long"
                    ),
                    isEntity = true
                ))
            }

            else -> {
                // Generic data model
                models.add(DataModelSpec(
                    name = "Item",
                    properties = listOf(
                        "id" to "String",
                        "name" to "String",
                        "description" to "String",
                        "createdAt" to "Long"
                    ),
                    isEntity = true
                ))
            }
        }

        return models
    }

    private fun planDependencies(requirements: ExtractedRequirements): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()

        // Core dependencies
        dependencies.add(Dependency("androidx.core:core-ktx", "Core Android"))
        dependencies.add(Dependency("androidx.lifecycle:lifecycle-viewmodel-compose", "ViewModel"))
        dependencies.add(Dependency("androidx.activity:activity-compose", "Activity"))
        dependencies.add(Dependency("androidx.compose.material3:material3", "Material3"))

        // Add based on requirements
        if (requirements.databaseNeed) {
            dependencies.add(Dependency("androidx.room:room-runtime", "Room Database"))
            dependencies.add(Dependency("androidx.room:room-ktx", "Room KTX"))
        }

        if (requirements.apiIntegration) {
            dependencies.add(Dependency("com.squareup.retrofit2:retrofit", "Retrofit"))
            dependencies.add(Dependency("com.squareup.retrofit2:converter-gson", "Gson Converter"))
        }

        if (requirements.primaryFeatures.any { it.contains("location") }) {
            dependencies.add(Dependency("com.google.android.gms:play-services-location", "Location Services"))
        }

        if (requirements.primaryFeatures.any { it.contains("camera") }) {
            dependencies.add(Dependency("androidx.camera:camera-camera2", "Camera"))
        }

        return dependencies
    }

    private fun planPermissions(requirements: ExtractedRequirements): List<String> {
        val permissions = mutableListOf<String>()

        if (requirements.apiIntegration) {
            permissions.add("android.permission.INTERNET")
            permissions.add("android.permission.ACCESS_NETWORK_STATE")
        }

        if (requirements.primaryFeatures.any { it.contains("location") }) {
            permissions.add("android.permission.ACCESS_FINE_LOCATION")
            permissions.add("android.permission.ACCESS_COARSE_LOCATION")
        }

        if (requirements.primaryFeatures.any { it.contains("camera") }) {
            permissions.add("android.permission.CAMERA")
        }

        if (requirements.offlineSupport) {
            permissions.add("android.permission.WRITE_EXTERNAL_STORAGE")
        }

        return permissions
    }

    private fun planColorScheme(requirements: ExtractedRequirements): ColorScheme {
        // Simple color scheme generation based on app type
        return when (requirements.appType) {
            AppType.WEATHER -> ColorScheme(
                primary = "#2196F3",    // Blue
                secondary = "#FFC107",  // Amber
                tertiary = "#4CAF50"    // Green
            )
            AppType.FITNESS -> ColorScheme(
                primary = "#FF5722",    // Deep Orange
                secondary = "#4CAF50",  // Green
                tertiary = "#2196F3"    // Blue
            )
            AppType.FINANCE -> ColorScheme(
                primary = "#4CAF50",    // Green
                secondary = "#2196F3",  // Blue
                tertiary = "#FFC107"    // Amber
            )
            else -> ColorScheme(
                primary = "#6200EA",    // Purple
                secondary = "#03DAC6",  // Teal
                tertiary = "#FF5722"    // Deep Orange
            )
        }
    }

    private fun planDatabaseSchema(
        requirements: ExtractedRequirements,
        dataModels: List<DataModelSpec>
    ): DatabaseSchema {
        if (!requirements.databaseNeed) {
            return DatabaseSchema(emptyList())
        }

        val tables = dataModels.filter { it.isEntity }.map { model ->
            DatabaseTable(
                name = model.name.lowercase() + "s",
                columns = model.properties.map { (name, type) ->
                    DatabaseColumn(name, mapToSqlType(type))
                }
            )
        }

        return DatabaseSchema(tables)
    }

    private fun planApiClients(requirements: ExtractedRequirements): List<ApiClientSpec> {
        if (!requirements.apiIntegration) {
            return emptyList()
        }

        return when (requirements.appType) {
            AppType.WEATHER -> listOf(
                ApiClientSpec(
                    name = "WeatherApiService",
                    baseUrl = "https://api.openweathermap.org/data/2.5/",
                    endpoints = listOf(
                        "GET weather - Get current weather",
                        "GET forecast - Get weather forecast"
                    )
                )
            )
            AppType.ECOMMERCE -> listOf(
                ApiClientSpec(
                    name = "EcommerceApiService",
                    baseUrl = "https://api.example.com/",
                    endpoints = listOf(
                        "GET products - Get product list",
                        "GET products/{id} - Get product details",
                        "POST orders - Create order"
                    )
                )
            )
            else -> listOf(
                ApiClientSpec(
                    name = "ApiService",
                    baseUrl = "https://api.example.com/",
                    endpoints = listOf("GET data - Get data")
                )
            )
        }
    }

    private fun planBuildConfig(requirements: ExtractedRequirements): BuildConfigSpec {
        return BuildConfigSpec(
            minSdk = 26,
            targetSdk = 34,
            versionCode = 1,
            versionName = "1.0.0",
            applicationId = generatePackageName(requirements.appName),
            buildTypes = listOf("debug", "release"),
            features = requirements.primaryFeatures + requirements.secondaryFeatures
        )
    }

    private fun generatePackageName(appName: String): String {
        val cleanName = appName.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
            .take(15)
        return "com.generated.$cleanName"
    }

    private fun mapToSqlType(kotlinType: String): String {
        return when (kotlinType) {
            "String" -> "TEXT"
            "Int", "Long" -> "INTEGER"
            "Double", "Float" -> "REAL"
            "Boolean" -> "INTEGER" // SQLite stores booleans as integers
            else -> "TEXT"
        }
    }
}