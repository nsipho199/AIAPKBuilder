package com.aiapkbuilder.app.util

import java.util.*

/**
 * Utility for generating unique identifiers and names for projects.
 */
object ProjectGenerationUtils {
    /**
     * Generate a unique project ID
     */
    fun generateProjectId(): String = UUID.randomUUID().toString()

    /**
     * Generate a build job ID
     */
    fun generateBuildJobId(): String = "build_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

    /**
     * Generate a template ID
     */
    fun generateTemplateId(): String = "template_${UUID.randomUUID().toString()}"

    /**
     * Convert app name to valid package name
     * Example: "My Weather App" -> "com.example.myweatherapp"
     */
    fun appNameToPackageName(appName: String): String {
        val nameWithoutSpecialChars = appName
            .lowercase()
            .replace("[^a-z0-9]".toRegex(), "")
            .replace("\\s+".toRegex(), "")

        return if (nameWithoutSpecialChars.isBlank()) {
            "com.example.app"
        } else {
            "com.example.${nameWithoutSpecialChars}"
        }
    }

    /**
     * Convert package name to a valid base package for Compose Navigation routes
     */
    fun packageNameToRouteBase(packageName: String): String =
        packageName.replace(".", "_").lowercase()

    /**
     * Generate a route for a screen
     */
    fun generateScreenRoute(screenName: String): String =
        screenName.lowercase().replace("\\s+".toRegex(), "_")

    /**
     * Validate package name format
     */
    fun isValidPackageName(packageName: String): Boolean {
        val pattern = "^[a-z][a-z0-9]*(\\.[a-z0-9]+)*$".toRegex()
        return packageName.matches(pattern) && packageName.split(".").size >= 2
    }

    /**
     * Validate app name
     */
    fun isValidAppName(appName: String): Boolean =
        appName.isNotBlank() && appName.length in 2..50 && !appName.matches("^[0-9].*".toRegex())

    /**
     * Get screen type enum from string
     */
    fun getScreenType(screenType: String): ScreenType = when (screenType.lowercase()) {
        "list" -> ScreenType.LIST
        "detail" -> ScreenType.DETAIL
        "form" -> ScreenType.FORM
        "settings" -> ScreenType.SETTINGS
        "dashboard" -> ScreenType.DASHBOARD
        "webview" -> ScreenType.WEBVIEW
        "map" -> ScreenType.MAP
        "camera" -> ScreenType.CAMERA
        else -> ScreenType.CUSTOM
    }
}

enum class ScreenType {
    LIST, DETAIL, FORM, SETTINGS, DASHBOARD, WEBVIEW, MAP, CAMERA, CUSTOM
}

/**
 * Container for generated project files
 */
data class GeneratedProjectFiles(
    val projectId: String,
    val buildGradle: String,
    val androidManifest: String,
    val navigationCode: String,
    val screens: Map<String, String>, // screenName -> compose code
    val viewModels: Map<String, String>, // vmName -> viewmodel code
    val repositories: Map<String, String>, // repoName -> repository code
    val dataModels: String,
    val database: String,
    val daoClasses: String,
    val theme: String,
    val strings: String
)

/**
 * Utility for file path management
 */
object FilePathUtils {
    fun getProjectSourcePath(projectId: String): String =
        "projects/$projectId/src/main"

    fun getComposePath(projectId: String): String =
        "${getProjectSourcePath(projectId)}/java/com/aiapkbuilder/generated"

    fun getResourcesPath(projectId: String): String =
        "${getProjectSourcePath(projectId)}/res"

    fun getStringsPath(projectId: String): String =
        "${getResourcesPath(projectId)}/values/strings.xml"

    fun getThemePath(projectId: String): String =
        "${getComposePath(projectId)}/theme/Theme.kt"

    fun getArtifactsPath(projectId: String): String =
        "artifacts/$projectId"

    fun getApkPath(projectId: String): String =
        "${getArtifactsPath(projectId)}/app-release.apk"

    fun getSourceZipPath(projectId: String): String =
        "${getArtifactsPath(projectId)}/source-${System.currentTimeMillis()}.zip"
}

/**
 * Size estimator for generated APKs
 */
object ApkSizeEstimator {
    /**
     * Estimate APK size based on app type and features
     * Returns size in KB
     */
    fun estimateSize(appType: String, featureCount: Int): Int {
        val baseSize = when (appType.lowercase()) {
            "calculator" -> 2500
            "notes" -> 2800
            "chat" -> 4200
            "ecommerce" -> 5500
            "delivery" -> 6000
            "taxi" -> 6500
            "school" -> 7000
            "ai_assistant" -> 4800
            "finance" -> 5200
            "productivity" -> 4500
            "portfolio" -> 3200
            "business" -> 4500
            "streaming" -> 8000
            "dashboard" -> 3500
            "weather" -> 3000
            "fitness" -> 3800
            "social" -> 7500
            else -> 4000
        }

        // Add ~200-300 KB per feature
        val featureSize = featureCount * 250
        return baseSize + featureSize
    }
}

/**
 * Estimated build times in seconds
 */
object BuildTimeEstimator {
    fun estimateBuildTime(appType: String, provider: String): Int {
        val baseTime = when (provider.lowercase()) {
            "github_actions" -> 300 // 5 min
            "codemagic" -> 180 // 3 min
            "docker" -> 240 // 4 min
            "self_hosted" -> 120 // 2 min (varies)
            "community" -> 600 // 10 min (varies)
            else -> 300
        }

        val typeMultiplier = when (appType.lowercase()) {
            "calculator" -> 0.8f
            "notes" -> 0.9f
            "chat" -> 1.2f
            "ecommerce" -> 1.5f
            "taxi" -> 1.5f
            "ai_assistant" -> 1.1f
            else -> 1.0f
        }

        return (baseTime * typeMultiplier).toInt()
    }
}
