package com.aiapkbuilder.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.aiapkbuilder.app.data.model.*
import com.aiapkbuilder.app.viewmodel.Quadruple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        // AI Settings
        val AI_PROVIDER = stringPreferencesKey("ai_provider")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val AI_BASE_URL = stringPreferencesKey("ai_base_url")
        val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
        val AI_TEMPERATURE = floatPreferencesKey("ai_temperature")

        // Build Settings
        val DEFAULT_BUILD_PROVIDER = stringPreferencesKey("default_build_provider")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val GITHUB_USERNAME = stringPreferencesKey("github_username")
        val CODEMAGIC_KEY = stringPreferencesKey("codemagic_key")
        val DOCKER_ENDPOINT = stringPreferencesKey("docker_endpoint")
        val SELF_HOSTED_URL = stringPreferencesKey("self_hosted_url")
        val COMMUNITY_NODE_URL = stringPreferencesKey("community_node_url")

        // UI Settings
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val AUTO_BUILD = booleanPreferencesKey("auto_build")
        val SHOW_ADVANCED = booleanPreferencesKey("show_advanced")

        // Cache & Cleanup
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        val AUTO_DELETE_OLD_BUILDS = booleanPreferencesKey("auto_delete_old_builds")
        val OLD_BUILDS_RETENTION_DAYS = intPreferencesKey("old_builds_retention_days")
    }

    // ─── Get All Settings ─────────────────────────────────────
    fun getSettings(): Flow<Quadruple<AISettings, BuildSettings, Boolean, Boolean>> =
        dataStore.data.map { prefs ->
            Quadruple(
                AISettings(
                    provider = AIProvider.entries.firstOrNull {
                        it.name == prefs[AI_PROVIDER]
                    } ?: AIProvider.OPENAI,
                    apiKey = prefs[AI_API_KEY] ?: "",
                    model = prefs[AI_MODEL] ?: "gpt-4o",
                    baseUrl = prefs[AI_BASE_URL] ?: "https://api.openai.com/v1/",
                    maxTokens = prefs[AI_MAX_TOKENS] ?: 4096,
                    temperature = prefs[AI_TEMPERATURE] ?: 0.7f
                ),
                BuildSettings(
                    defaultProvider = BuildProvider.entries.firstOrNull {
                        it.name == prefs[DEFAULT_BUILD_PROVIDER]
                    } ?: BuildProvider.GITHUB_ACTIONS,
                    githubToken = prefs[GITHUB_TOKEN] ?: "",
                    githubUsername = prefs[GITHUB_USERNAME] ?: "",
                    codemagicApiKey = prefs[CODEMAGIC_KEY] ?: "",
                    dockerEndpoint = prefs[DOCKER_ENDPOINT] ?: "",
                    selfHostedEndpoint = prefs[SELF_HOSTED_URL] ?: "",
                    communityNodeEndpoint = prefs[COMMUNITY_NODE_URL] ?: "https://community.aiapkbuilder.io"
                ),
                prefs[DARK_MODE] ?: false,
                prefs[AUTO_BUILD] ?: true
            )
        }

    // ─── Save All Settings ────────────────────────────────────
    suspend fun saveSettings(
        ai: AISettings,
        build: BuildSettings,
        darkMode: Boolean,
        autoBuild: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[AI_PROVIDER] = ai.provider.name
            prefs[AI_API_KEY] = ai.apiKey
            prefs[AI_MODEL] = ai.model
            prefs[AI_BASE_URL] = ai.baseUrl
            prefs[AI_MAX_TOKENS] = ai.maxTokens
            prefs[AI_TEMPERATURE] = ai.temperature
            prefs[DEFAULT_BUILD_PROVIDER] = build.defaultProvider.name
            prefs[GITHUB_TOKEN] = build.githubToken
            prefs[GITHUB_USERNAME] = build.githubUsername
            prefs[CODEMAGIC_KEY] = build.codemagicApiKey
            prefs[DOCKER_ENDPOINT] = build.dockerEndpoint
            prefs[SELF_HOSTED_URL] = build.selfHostedEndpoint
            prefs[COMMUNITY_NODE_URL] = build.communityNodeEndpoint
            prefs[DARK_MODE] = darkMode
            prefs[AUTO_BUILD] = autoBuild
        }
    }

    // ─── AI Settings ──────────────────────────────────────────
    fun getAISettings(): Flow<AISettings> = dataStore.data.map { prefs ->
        AISettings(
            provider = AIProvider.entries.firstOrNull { it.name == prefs[AI_PROVIDER] } ?: AIProvider.OPENAI,
            apiKey = prefs[AI_API_KEY] ?: "",
            model = prefs[AI_MODEL] ?: "gpt-4o",
            baseUrl = prefs[AI_BASE_URL] ?: "https://api.openai.com/v1/",
            maxTokens = prefs[AI_MAX_TOKENS] ?: 4096,
            temperature = prefs[AI_TEMPERATURE] ?: 0.7f
        )
    }

    suspend fun saveAISettings(settings: AISettings) {
        dataStore.edit { prefs ->
            prefs[AI_PROVIDER] = settings.provider.name
            prefs[AI_API_KEY] = settings.apiKey
            prefs[AI_MODEL] = settings.model
            prefs[AI_BASE_URL] = settings.baseUrl
            prefs[AI_MAX_TOKENS] = settings.maxTokens
            prefs[AI_TEMPERATURE] = settings.temperature
        }
    }

    suspend fun updateAIApiKey(key: String) {
        dataStore.edit { prefs -> prefs[AI_API_KEY] = key }
    }

    // ─── Build Settings ───────────────────────────────────────
    fun getBuildSettings(): Flow<BuildSettings> = dataStore.data.map { prefs ->
        BuildSettings(
            defaultProvider = BuildProvider.entries.firstOrNull {
                it.name == prefs[DEFAULT_BUILD_PROVIDER]
            } ?: BuildProvider.GITHUB_ACTIONS,
            githubToken = prefs[GITHUB_TOKEN] ?: "",
            githubUsername = prefs[GITHUB_USERNAME] ?: "",
            codemagicApiKey = prefs[CODEMAGIC_KEY] ?: "",
            dockerEndpoint = prefs[DOCKER_ENDPOINT] ?: "",
            selfHostedEndpoint = prefs[SELF_HOSTED_URL] ?: "",
            communityNodeEndpoint = prefs[COMMUNITY_NODE_URL] ?: "https://community.aiapkbuilder.io"
        )
    }

    suspend fun saveBuildSettings(settings: BuildSettings) {
        dataStore.edit { prefs ->
            prefs[DEFAULT_BUILD_PROVIDER] = settings.defaultProvider.name
            prefs[GITHUB_TOKEN] = settings.githubToken
            prefs[GITHUB_USERNAME] = settings.githubUsername
            prefs[CODEMAGIC_KEY] = settings.codemagicApiKey
            prefs[DOCKER_ENDPOINT] = settings.dockerEndpoint
            prefs[SELF_HOSTED_URL] = settings.selfHostedEndpoint
            prefs[COMMUNITY_NODE_URL] = settings.communityNodeEndpoint
        }
    }

    suspend fun updateGitHubCredentials(token: String, username: String) {
        dataStore.edit { prefs ->
            prefs[GITHUB_TOKEN] = token
            prefs[GITHUB_USERNAME] = username
        }
    }

    // ─── UI Settings ──────────────────────────────────────────
    fun isDarkModeEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[DARK_MODE] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DARK_MODE] = enabled }
    }

    fun isAutoBuildEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[AUTO_BUILD] ?: true }

    suspend fun setAutoBuild(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_BUILD] = enabled }
    }

    fun isMaterialYouEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[MATERIAL_YOU] ?: false }

    suspend fun setMaterialYou(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[MATERIAL_YOU] = enabled }
    }

    fun isShowAdvancedEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[SHOW_ADVANCED] ?: false }

    suspend fun setShowAdvanced(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SHOW_ADVANCED] = enabled }
    }

    // ─── Cache Settings ───────────────────────────────────────
    fun isCacheEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[CACHE_ENABLED] ?: true }

    suspend fun setCacheEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[CACHE_ENABLED] = enabled }
    }

    fun isAutoDeleteEnabled(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[AUTO_DELETE_OLD_BUILDS] ?: false }

    suspend fun setAutoDelete(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[AUTO_DELETE_OLD_BUILDS] = enabled }
    }

    fun getBuildRetentionDays(): Flow<Int> =
        dataStore.data.map { prefs -> prefs[OLD_BUILDS_RETENTION_DAYS] ?: 30 }

    suspend fun setBuildRetentionDays(days: Int) {
        dataStore.edit { prefs -> prefs[OLD_BUILDS_RETENTION_DAYS] = days }
    }

    // ─── Clear All Settings ───────────────────────────────────
    suspend fun clearAllSettings() {
        dataStore.edit { prefs -> prefs.clear() }
    }
}

