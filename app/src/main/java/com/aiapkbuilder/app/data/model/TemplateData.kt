package com.aiapkbuilder.app.data.model

/**
 * Pre-built templates for different app types
 * Used as scaffolding for code generation
 */
object TemplateFactory {
    fun getBuiltInTemplates(): List<ProjectTemplate> = listOf(
        // Calculator
        ProjectTemplate(
            id = "calc_basic",
            name = "Basic Calculator",
            appType = AppType.CALCULATOR,
            description = "Simple calculator with basic operations",
            category = "Utility",
            difficulty = "beginner",
            features = listOf("Basic Math", "History", "Dark Mode"),
            screenCount = 2,
            estimatedBuildTime = 120,
            baseGradleTemplate = calculatorGradleTemplate(),
            screensTemplate = calculatorScreensTemplate(),
            isBuiltIn = true
        ),

        // Notes
        ProjectTemplate(
            id = "notes_basic",
            name = "Notes App",
            appType = AppType.NOTES,
            description = "Simple note-taking application",
            category = "Productivity",
            difficulty = "beginner",
            features = listOf("Create Notes", "Edit Notes", "Delete Notes", "Search"),
            screenCount = 2,
            estimatedBuildTime = 180,
            baseGradleTemplate = notesGradleTemplate(),
            screensTemplate = notesScreensTemplate(),
            isBuiltIn = true
        ),

        // Chat
        ProjectTemplate(
            id = "chat_basic",
            name = "Chat Application",
            appType = AppType.CHAT,
            description = "Simple chat messaging app",
            category = "Communication",
            difficulty = "intermediate",
            features = listOf("Real-time Chat", "User Profiles", "Message History", "Typing Indicator"),
            screenCount = 3,
            estimatedBuildTime = 300,
            baseGradleTemplate = chatGradleTemplate(),
            screensTemplate = chatScreensTemplate(),
            isBuiltIn = true
        ),

        // Weather
        ProjectTemplate(
            id = "weather_basic",
            name = "Weather App",
            appType = AppType.WEATHER,
            description = "Weather forecast application",
            category = "Lifestyle",
            difficulty = "intermediate",
            features = listOf("Current Weather", "Forecast", "Location Search", "Alerts"),
            screenCount = 2,
            estimatedBuildTime = 240,
            baseGradleTemplate = weatherGradleTemplate(),
            screensTemplate = weatherScreensTemplate(),
            isBuiltIn = true
        ),

        // E-Commerce
        ProjectTemplate(
            id = "ecommerce_basic",
            name = "E-Commerce Store",
            appType = AppType.ECOMMERCE,
            description = "Basic online shopping application",
            category = "Business",
            difficulty = "advanced",
            features = listOf("Product Catalog", "Shopping Cart", "Checkout", "Order Tracking", "Payment"),
            screenCount = 5,
            estimatedBuildTime = 480,
            baseGradleTemplate = ecommerceGradleTemplate(),
            screensTemplate = ecommerceScreensTemplate(),
            isBuiltIn = true
        ),

        // Fitness
        ProjectTemplate(
            id = "fitness_basic",
            name = "Fitness Tracker",
            appType = AppType.FITNESS,
            description = "Health and fitness tracking app",
            category = "Health",
            difficulty = "intermediate",
            features = listOf("Workout Tracking", "Progress Charts", "Goals", "Stats"),
            screenCount = 4,
            estimatedBuildTime = 300,
            baseGradleTemplate = fitnessGradleTemplate(),
            screensTemplate = fitnessScreensTemplate(),
            isBuiltIn = true
        )
    )

    private fun calculatorGradleTemplate(): String = """
        dependencies {
            implementation 'androidx.compose.material3:material3:1.1.2'
            implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1'
        }
    """.trimIndent()

    private fun notesGradleTemplate(): String = """
        dependencies {
            implementation 'androidx.room:room-runtime:2.6.1'
            implementation 'androidx.room:room-ktx:2.6.1'
            implementation 'androidx.compose.material3:material3:1.1.2'
        }
    """.trimIndent()

    private fun chatGradleTemplate(): String = """
        dependencies {
            implementation 'androidx.room:room-runtime:2.6.1'
            implementation 'com.squareup.retrofit2:retrofit:2.11.0'
            implementation 'io.socket:socket.io-client:2.1.1'
        }
    """.trimIndent()

    private fun weatherGradleTemplate(): String = """
        dependencies {
            implementation 'com.squareup.retrofit2:retrofit:2.11.0'
            implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
            implementation 'com.google.android.gms:play-services-location:21.0.1'
        }
    """.trimIndent()

    private fun ecommerceGradleTemplate(): String = """
        dependencies {
            implementation 'androidx.room:room-runtime:2.6.1'
            implementation 'com.squareup.retrofit2:retrofit:2.11.0'
            implementation 'com.stripe:stripe-android:20.1.0'
            implementation 'com.google.android.gms:play-services-maps:18.1.0'
        }
    """.trimIndent()

    private fun fitnessGradleTemplate(): String = """
        dependencies {
            implementation 'androidx.room:room-runtime:2.6.1'
            implementation 'com.google.android.gms:play-services-fitness:21.1.0'
            implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
        }
    """.trimIndent()

    private fun calculatorScreensTemplate(): String = """
        @Composable
        fun CalculatorScreen() {
            // Calculator UI composable
        }
        
        @Composable
        fun HistoryScreen() {
            // History UI composable
        }
    """.trimIndent()

    private fun notesScreensTemplate(): String = """
        @Composable
        fun NotesListScreen() {
            // Notes list composable
        }
        
        @Composable
        fun NoteDetailScreen(noteId: String) {
            // Note detail composable
        }
    """.trimIndent()

    private fun chatScreensTemplate(): String = """
        @Composable
        fun ChatListScreen() {
            // Chat list composable
        }
        
        @Composable
        fun ChatDetailScreen(chatId: String) {
            // Chat detail composable
        }
        
        @Composable
        fun UserProfileScreen(userId: String) {
            // User profile composable
        }
    """.trimIndent()

    private fun weatherScreensTemplate(): String = """
        @Composable
        fun CurrentWeatherScreen() {
            // Current weather composable
        }
        
        @Composable
        fun ForecastScreen() {
            // Forecast composable
        }
    """.trimIndent()

    private fun ecommerceScreensTemplate(): String = """
        @Composable
        fun ProductCatalogScreen() {
            // Product list composable
        }
        
        @Composable
        fun ProductDetailScreen(productId: String) {
            // Product detail composable
        }
        
        @Composable
        fun CartScreen() {
            // Shopping cart composable
        }
        
        @Composable
        fun CheckoutScreen() {
            // Checkout composable
        }
        
        @Composable
        fun OrderTrackingScreen() {
            // Order tracking composable
        }
    """.trimIndent()

    private fun fitnessScreensTemplate(): String = """
        @Composable
        fun WorkoutListScreen() {
            // Workout list composable
        }
        
        @Composable
        fun StatsScreen() {
            // Statistics composable
        }
        
        @Composable
        fun GoalsScreen() {
            // Goals composable
        }
        
        @Composable
        fun ProgressScreen() {
            // Progress charts composable
        }
    """.trimIndent()
}

/**
 * API endpoint specifications for different services
 */
object ApiEndpointTemplates {
    fun getEndpointsForAppType(appType: AppType): List<String> = when (appType) {
        AppType.WEATHER -> listOf(
            "/api/weather/current",
            "/api/weather/forecast",
            "/api/locations/search"
        )
        AppType.CHAT -> listOf(
            "/api/messages/send",
            "/api/messages/history",
            "/api/users/list",
            "/api/users/profile"
        )
        AppType.ECOMMERCE -> listOf(
            "/api/products/list",
            "/api/products/:id",
            "/api/orders/create",
            "/api/orders/track",
            "/api/cart/add",
            "/api/cart/remove"
        )
        AppType.TAXI -> listOf(
            "/api/rides/request",
            "/api/rides/cancel",
            "/api/rides/status",
            "/api/drivers/nearby",
            "/api/payment/process"
        )
        AppType.DELIVERY -> listOf(
            "/api/orders/create",
            "/api/orders/track",
            "/api/restaurants/list",
            "/api/restaurants/menu",
            "/api/payment/process"
        )
        else -> listOf("/api/data/list", "/api/data/:id")
    }
}

/**
 * Database schema templates
 */
object DatabaseTemplates {
    fun getTablesForAppType(appType: AppType): List<String> = when (appType) {
        AppType.NOTES -> listOf("notes", "tags", "note_tags")
        AppType.CHAT -> listOf("conversations", "messages", "users", "user_status")
        AppType.ECOMMERCE -> listOf("products", "categories", "orders", "cart_items", "reviews")
        AppType.WEATHER -> listOf("locations", "weather_cache", "favorites")
        AppType.FITNESS -> listOf("workouts", "exercises", "goals", "measurements")
        AppType.TAXI -> listOf("rides", "drivers", "users", "ratings")
        AppType.DELIVERY -> listOf("orders", "restaurants", "menu_items", "reviews")
        else -> listOf("data")
    }
}
