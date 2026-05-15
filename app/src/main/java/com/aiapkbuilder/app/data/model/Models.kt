package com.aiapkbuilder.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.Serializable

// ─── App Project ───────────────────────────────────────────────
@Entity(tableName = "projects")
@TypeConverters(StringListConverter::class, MapConverters::class)
data class AppProject(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val appType: AppType,
    val buildStatus: BuildStatus = BuildStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val packageName: String = "",
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val minSdk: Int = 26,
    val targetSdk: Int = 35,
    val features: List<String> = emptyList(),
    val screens: List<String> = emptyList(),
    val apkPath: String? = null,
    val sourceZipPath: String? = null,
    val buildLogs: String = "",
    val errorMessage: String? = null,
    val buildProvider: BuildProvider = BuildProvider.LOCAL,
    val githubRepo: String? = null,
    val codemagicBuildId: String? = null,
    val estimatedSizeKb: Int = 0,
    val iconColor: String = "#6200EA",
    val generatedProjectPlan: GeneratedProjectPlan? = null,
    val metadata: Map<String, String> = emptyMap(),
    val isFavorite: Boolean = false,
    val lastBuildJobId: String? = null,
    val totalBuilds: Int = 0
)

// ─── AI Generation Request/Response ────────────────────────────
data class GenerationRequest(
    val prompt: String,
    val appType: AppType? = null,
    val additionalFeatures: List<String> = emptyList(),
    val targetAudience: String = "",
    val colorScheme: String = ""
)

data class GeneratedProjectPlan(
    val appName: String,
    val packageName: String,
    val description: String,
    val appType: AppType,
    val screens: List<ScreenSpec>,
    val features: List<String>,
    val dependencies: List<String>,
    val colorPrimary: String,
    val colorSecondary: String,
    val minSdkVersion: Int,
    val permissions: List<String>,
    val navigationStructure: String,
    val databaseTables: List<String>,
    val apiEndpoints: List<String>
)

data class ScreenSpec(
    val name: String,
    val route: String,
    val description: String,
    val uiComponents: List<String>,
    val hasBottomNav: Boolean = false
)

// ─── Build Job ─────────────────────────────────────────────────
@Entity(tableName = "build_jobs")
data class BuildJob(
    @PrimaryKey val jobId: String,
    val projectId: String,
    val provider: BuildProvider,
    val status: BuildStatus,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val logOutput: String = "",
    val artifactUrl: String? = null,
    val errorMessage: String? = null,
    val progressPercent: Int = 0
)

// ─── AI Settings ───────────────────────────────────────────────
data class AISettings(
    val provider: AIProvider = AIProvider.OPENAI,
    val apiKey: String = "",
    val model: String = "gpt-4o",
    val baseUrl: String = "https://api.openai.com/v1/",
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f
)

// ─── Build Settings ────────────────────────────────────────────
data class BuildSettings(
    val defaultProvider: BuildProvider = BuildProvider.GITHUB_ACTIONS,
    val githubToken: String = "",
    val githubUsername: String = "",
    val codemagicApiKey: String = "",
    val dockerEndpoint: String = "",
    val selfHostedEndpoint: String = "",
    val communityNodeEndpoint: String = "https://community.aiapkbuilder.io"
)

// ─── Enums ─────────────────────────────────────────────────────
enum class AppType(val displayName: String, val icon: String) {
    CALCULATOR("Calculator", "calculate"),
    NOTES("Notes App", "note"),
    CHAT("Chat App", "chat"),
    ECOMMERCE("E-Commerce", "shopping_cart"),
    DELIVERY("Delivery App", "delivery_dining"),
    TAXI("Taxi Booking", "local_taxi"),
    SCHOOL("School System", "school"),
    AI_ASSISTANT("AI Assistant", "smart_toy"),
    FINANCE("Finance App", "account_balance"),
    PRODUCTIVITY("Productivity", "task_alt"),
    PORTFOLIO("Portfolio", "work"),
    BUSINESS("Business App", "business"),
    STREAMING("Streaming UI", "play_circle"),
    DASHBOARD("Dashboard", "dashboard"),
    WEATHER("Weather App", "cloud"),
    FITNESS("Fitness App", "fitness_center"),
    SOCIAL("Social App", "group"),
    CUSTOM("Custom", "code")
}

enum class BuildStatus(val displayName: String) {
    PENDING("Pending"),
    GENERATING("Generating Code"),
    BUILDING("Building APK"),
    SUCCESS("Ready"),
    FAILED("Failed"),
    CANCELLED("Cancelled")
}

enum class BuildProvider(val displayName: String) {
    LOCAL("Local Build"),
    GITHUB_ACTIONS("GitHub Actions"),
    CODEMAGIC("Codemagic"),
    DOCKER("Docker"),
    SELF_HOSTED("Self-Hosted"),
    COMMUNITY("Community Node")
}

enum class AIProvider(val displayName: String) {
    OPENAI("OpenAI"),
    OPENROUTER("OpenRouter"),
    OLLAMA("Ollama (Local)"),
    GROQ("Groq"),
    CUSTOM("Custom API")
}

// ─── Template & Code Generation ────────────────────────────────
@Entity(tableName = "templates")
data class ProjectTemplate(
    @PrimaryKey val id: String,
    val name: String,
    val appType: AppType,
    val description: String,
    val category: String,
    val difficulty: String, // "beginner", "intermediate", "advanced"
    val features: List<String>,
    val screenCount: Int,
    val estimatedBuildTime: Int, // in seconds
    val baseGradleTemplate: String, // Gradle config template
    val screensTemplate: String, // Navigation/screens template
    val createdAt: Long = System.currentTimeMillis(),
    val isBuiltIn: Boolean = false
)

// ─── Build Configuration ───────────────────────────────────────
@Entity(tableName = "build_configs")
@TypeConverters(MapConverters::class)
data class BuildConfig(
    @PrimaryKey val id: String,
    val projectId: String,
    val provider: BuildProvider,
    val configJson: String, // JSON-serialized provider-specific config
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

// ─── Code Generation Cache ─────────────────────────────────────
@Entity(tableName = "code_cache")
data class CodeGenerationCache(
    @PrimaryKey val id: String,
    val projectId: String,
    val screenName: String,
    val generatedCode: String,
    val codeType: String, // "compose", "viewmodel", "repository", etc.
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24h TTL
)

// ─── Build Artifact ────────────────────────────────────────────
@Entity(tableName = "artifacts")
data class BuildArtifact(
    @PrimaryKey val id: String,
    val buildJobId: String,
    val projectId: String,
    val artifactType: String, // "apk", "aab", "source", "aar"
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val fileName: String,
    val fileSizeBytes: Long,
    val sha256Hash: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Type Converters ───────────────────────────────────────────
class StringListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}

class MapConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromMap(value: Map<String, String>): String = gson.toJson(value)

    @TypeConverter
    fun toMap(value: String): Map<String, String> {
        val type = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }
}
