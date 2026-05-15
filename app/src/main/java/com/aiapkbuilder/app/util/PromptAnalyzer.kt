package com.aiapkbuilder.app.util

import com.aiapkbuilder.app.data.model.AppType
import com.aiapkbuilder.app.data.model.Complexity
import com.aiapkbuilder.app.util.safeExecute
import java.util.regex.Pattern

/**
 * Analyzes natural language prompts to extract app requirements.
 * Uses pattern matching and keyword analysis to understand user intent.
 */
class PromptAnalyzer {

    /**
     * Analyzes a user prompt and extracts structured requirements.
     */
    fun analyzePrompt(prompt: String): Result<ExtractedRequirements> = safeExecute {
        val normalizedPrompt = prompt.lowercase().trim()

        // Extract app type
        val appType = detectAppType(normalizedPrompt)

        // Extract features
        val features = extractFeatures(normalizedPrompt)

        // Determine complexity
        val complexity = determineComplexity(features.size, normalizedPrompt)

        // Extract target audience
        val targetAudience = extractTargetAudience(normalizedPrompt)

        // Estimate screen count
        val estimatedScreens = estimateScreenCount(features, appType)

        // Check for API needs
        val needsAPI = checkAPINeeds(normalizedPrompt, features)

        // Check for database needs
        val needsDatabase = checkDatabaseNeeds(normalizedPrompt, features)

        // Check for offline support
        val needsOffline = checkOfflineNeeds(normalizedPrompt)

        // Generate app name
        val appName = generateAppName(normalizedPrompt, appType)

        ExtractedRequirements(
            appName = appName,
            appType = appType,
            primaryFeatures = features.take(3), // Top 3 features
            secondaryFeatures = features.drop(3), // Remaining features
            targetAudience = targetAudience,
            complexity = complexity,
            estimatedScreenCount = estimatedScreens,
            databaseNeed = needsDatabase,
            apiIntegration = needsAPI,
            offlineSupport = needsOffline
        )
    }

    private fun detectAppType(prompt: String): AppType {
        // Define patterns for each app type
        val patterns = mapOf(
            AppType.WEATHER to Pattern.compile("\\b(weather|forecast|temperature|climate)\\b"),
            AppType.NOTES to Pattern.compile("\\b(note|todo|task|reminder|journal)\\b"),
            AppType.CHAT to Pattern.compile("\\b(chat|message|conversation|messaging|social)\\b"),
            AppType.CALCULATOR to Pattern.compile("\\b(calculator|calculate|math|compute)\\b"),
            AppType.ECOMMERCE to Pattern.compile("\\b(shop|store|ecommerce|buy|sell|product|cart|checkout)\\b"),
            AppType.TAXI to Pattern.compile("\\b(taxi|ride|uber|lyft|transport|booking)\\b"),
            AppType.DELIVERY to Pattern.compile("\\b(delivery|food|restaurant|order|pickup)\\b"),
            AppType.FITNESS to Pattern.compile("\\b(fitness|workout|exercise|health|gym|training)\\b"),
            AppType.FINANCE to Pattern.compile("\\b(finance|money|budget|expense|bank|wallet)\\b"),
            AppType.PRODUCTIVITY to Pattern.compile("\\b(productivity|task|schedule|calendar|organizer)\\b"),
            AppType.SCHOOL to Pattern.compile("\\b(school|student|teacher|education|learning)\\b"),
            AppType.AI_ASSISTANT to Pattern.compile("\\b(ai|assistant|bot|intelligent|smart)\\b"),
            AppType.STREAMING to Pattern.compile("\\b(stream|video|music|media|player)\\b"),
            AppType.DASHBOARD to Pattern.compile("\\b(dashboard|analytics|stats|metrics|report)\\b"),
            AppType.PORTFOLIO to Pattern.compile("\\b(portfolio|cv|resume|profile|showcase)\\b"),
            AppType.BUSINESS to Pattern.compile("\\b(business|company|enterprise|corporate)\\b"),
            AppType.SOCIAL to Pattern.compile("\\b(social|network|friend|community|connect)\\b")
        )

        // Find the best match
        var bestMatch = AppType.CUSTOM
        var highestScore = 0

        patterns.forEach { (type, pattern) ->
            val matcher = pattern.matcher(prompt)
            var score = 0
            while (matcher.find()) {
                score++
            }
            if (score > highestScore) {
                highestScore = score
                bestMatch = type
            }
        }

        return bestMatch
    }

    private fun extractFeatures(prompt: String): List<String> {
        val features = mutableListOf<String>()

        // Common feature patterns
        val featurePatterns = listOf(
            "user authentication" to Pattern.compile("\\b(login|signup|register|auth)\\b"),
            "search functionality" to Pattern.compile("\\b(search|find|filter)\\b"),
            "notifications" to Pattern.compile("\\b(notification|alert|push)\\b"),
            "favorites/bookmarks" to Pattern.compile("\\b(favorite|bookmark|save|like)\\b"),
            "sharing" to Pattern.compile("\\b(share|export|social)\\b"),
            "settings" to Pattern.compile("\\b(setting|preference|config)\\b"),
            "dark mode" to Pattern.compile("\\b(dark|theme|night)\\b"),
            "offline support" to Pattern.compile("\\b(offline|cache|sync)\\b"),
            "real-time updates" to Pattern.compile("\\b(real.?time|live|update)\\b"),
            "location services" to Pattern.compile("\\b(location|gps|map)\\b"),
            "camera integration" to Pattern.compile("\\b(camera|photo|image)\\b"),
            "payment processing" to Pattern.compile("\\b(payment|pay|stripe|checkout)\\b"),
            "reviews/ratings" to Pattern.compile("\\b(review|rating|star|feedback)\\b"),
            "admin panel" to Pattern.compile("\\b(admin|manage|dashboard)\\b"),
            "multi-language" to Pattern.compile("\\b(language|translate|i18n)\\b")
        )

        featurePatterns.forEach { (feature, pattern) ->
            if (pattern.matcher(prompt).find()) {
                features.add(feature)
            }
        }

        // Add app-type specific features
        when (detectAppType(prompt)) {
            AppType.WEATHER -> features.addAll(listOf(
                "current weather", "forecast", "weather alerts"
            ))
            AppType.NOTES -> features.addAll(listOf(
                "create notes", "edit notes", "delete notes", "categories"
            ))
            AppType.CHAT -> features.addAll(listOf(
                "send messages", "user profiles", "message history"
            ))
            AppType.ECOMMERCE -> features.addAll(listOf(
                "product catalog", "shopping cart", "checkout process"
            ))
            AppType.FITNESS -> features.addAll(listOf(
                "workout tracking", "progress charts", "goals"
            ))
        }

        return features.distinct()
    }

    private fun determineComplexity(featureCount: Int, prompt: String): Complexity {
        return when {
            featureCount >= 8 || prompt.contains("complex") || prompt.contains("advanced") -> Complexity.COMPLEX
            featureCount >= 5 || prompt.contains("moderate") -> Complexity.MODERATE
            else -> Complexity.SIMPLE
        }
    }

    private fun extractTargetAudience(prompt: String): String {
        val audiences = listOf(
            "students" to Pattern.compile("\\b(student|school|education|learn)\\b"),
            "professionals" to Pattern.compile("\\b(professional|business|work|office)\\b"),
            "families" to Pattern.compile("\\b(family|parent|child|home)\\b"),
            "fitness enthusiasts" to Pattern.compile("\\b(fitness|workout|gym|health)\\b"),
            "shoppers" to Pattern.compile("\\b(shop|buy|ecommerce|store)\\b"),
            "developers" to Pattern.compile("\\b(developer|code|program|tech)\\b"),
            "general users" to Pattern.compile("\\b(user|people|everyone|general)\\b")
        )

        audiences.forEach { (audience, pattern) ->
            if (pattern.matcher(prompt).find()) {
                return audience
            }
        }

        return "general users"
    }

    private fun estimateScreenCount(features: List<String>, appType: AppType): Int {
        val baseScreens = when (appType) {
            AppType.CALCULATOR -> 1
            AppType.NOTES -> 2
            AppType.WEATHER -> 2
            AppType.CHAT -> 3
            AppType.ECOMMERCE -> 5
            AppType.FITNESS -> 4
            AppType.FINANCE -> 3
            else -> 3
        }

        val featureScreens = features.size / 2 // Roughly 2 features per screen
        return (baseScreens + featureScreens).coerceAtMost(8) // Max 8 screens
    }

    private fun checkAPINeeds(prompt: String, features: List<String>): Boolean {
        val apiIndicators = listOf(
            "weather", "forecast", "api", "external", "service",
            "payment", "stripe", "location", "maps", "social"
        )

        return apiIndicators.any { indicator ->
            prompt.contains(indicator) || features.any { it.contains(indicator) }
        }
    }

    private fun checkDatabaseNeeds(prompt: String, features: List<String>): Boolean {
        val dbIndicators = listOf(
            "save", "store", "data", "database", "persistent",
            "history", "favorites", "cache", "local"
        )

        return dbIndicators.any { indicator ->
            prompt.contains(indicator) || features.any { it.contains(indicator) }
        }
    }

    private fun checkOfflineNeeds(prompt: String): Boolean {
        val offlineIndicators = listOf(
            "offline", "cache", "sync", "local", "no internet"
        )

        return offlineIndicators.any { prompt.contains(it) }
    }

    private fun generateAppName(prompt: String, appType: AppType): String {
        // Try to extract a name from the prompt
        val namePatterns = listOf(
            Pattern.compile("call(?:ed|ing it|\\s+)['\"]?([A-Za-z][A-Za-z0-9\\s]{1,20})['\"]?"),
            Pattern.compile("name(?:d|ing it|\\s+)['\"]?([A-Za-z][A-Za-z0-9\\s]{1,20})['\"]?"),
            Pattern.compile("app\\s+(?:called|named)\\s+['\"]?([A-Za-z][A-Za-z0-9\\s]{1,20})['\"]?")
        )

        namePatterns.forEach { pattern ->
            val matcher = pattern.matcher(prompt)
            if (matcher.find()) {
                return matcher.group(1)?.trim()?.capitalize() ?: ""
            }
        }

        // Fallback to type-based name
        return when (appType) {
            AppType.WEATHER -> "WeatherPro"
            AppType.NOTES -> "QuickNotes"
            AppType.CHAT -> "ChatApp"
            AppType.CALCULATOR -> "SmartCalc"
            AppType.ECOMMERCE -> "ShopHub"
            AppType.FITNESS -> "FitTracker"
            AppType.FINANCE -> "MoneyWise"
            else -> "MyApp"
        }
    }
}

/**
 * Data class representing extracted requirements from a prompt.
 */
data class ExtractedRequirements(
    val appName: String,
    val appType: AppType,
    val primaryFeatures: List<String>,
    val secondaryFeatures: List<String>,
    val targetAudience: String,
    val complexity: Complexity,
    val estimatedScreenCount: Int,
    val databaseNeed: Boolean,
    val apiIntegration: Boolean,
    val offlineSupport: Boolean
)