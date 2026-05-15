package com.aiapkbuilder.app.data.service

import com.aiapkbuilder.app.data.api.OpenAIApiService
import com.aiapkbuilder.app.data.model.AIProvider
import com.aiapkbuilder.app.data.model.AISettings
import com.aiapkbuilder.app.data.repository.SettingsRepository
import com.aiapkbuilder.app.util.AppLogger
import com.aiapkbuilder.app.util.safeExecuteAsync
import kotlinx.coroutines.flow.first
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages multiple AI providers with fallback support.
 * Handles provider selection, token management, and error recovery.
 */
@Singleton
class AIProviderManager @Inject constructor(
    private val openAIService: OpenAIApiService,
    private val settingsRepository: SettingsRepository
) {
    private val logger = AppLogger

    /**
     * Executes a prompt with automatic provider fallback.
     * Tries primary provider first, falls back to secondary if available.
     */
    suspend fun executePrompt(
        prompt: String,
        systemMessage: String? = null,
        maxTokens: Int = 4096,
        temperature: Float = 0.7f
    ): Result<String> = safeExecuteAsync {
        val settings = settingsRepository.getAISettings().first()

        // Try primary provider
        val primaryResult = executeWithProvider(settings, prompt, systemMessage, maxTokens, temperature)
        if (primaryResult.isSuccess) {
            return@safeExecuteAsync primaryResult.getOrThrow()
        }

        // If primary fails and we have a fallback, try it
        val fallbackProvider = getFallbackProvider(settings.provider)
        if (fallbackProvider != null) {
            logger.w("Primary provider ${settings.provider} failed, trying fallback $fallbackProvider")
            val fallbackSettings = settings.copy(provider = fallbackProvider)
            val fallbackResult = executeWithProvider(fallbackSettings, prompt, systemMessage, maxTokens, temperature)
            if (fallbackResult.isSuccess) {
                return@safeExecuteAsync fallbackResult.getOrThrow()
            }
        }

        // All providers failed
        throw Exception("All AI providers failed. Primary: ${primaryResult.exceptionOrNull()?.message}")
    }

    private suspend fun executeWithProvider(
        settings: AISettings,
        prompt: String,
        systemMessage: String?,
        maxTokens: Int,
        temperature: Float
    ): Result<String> = safeExecuteAsync {
        when (settings.provider) {
            AIProvider.OPENAI -> executeOpenAI(settings, prompt, systemMessage, maxTokens, temperature)
            AIProvider.OPENROUTER -> executeOpenRouter(settings, prompt, systemMessage, maxTokens, temperature)
            AIProvider.GROQ -> executeGroq(settings, prompt, systemMessage, maxTokens, temperature)
            AIProvider.OLLAMA -> executeOllama(settings, prompt, systemMessage, maxTokens, temperature)
            AIProvider.CUSTOM -> executeCustom(settings, prompt, systemMessage, maxTokens, temperature)
        }
    }

    private suspend fun executeOpenAI(
        settings: AISettings,
        prompt: String,
        systemMessage: String?,
        maxTokens: Int,
        temperature: Float
    ): String {
        val messages = buildMessages(prompt, systemMessage)
        val request = OpenAIRequest(
            model = settings.model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature
        )

        val response = openAIService.generateCompletion(
            authorization = "Bearer ${settings.apiKey}",
            request = request
        )

        return handleOpenAIResponse(response)
    }

    private suspend fun executeOpenRouter(
        settings: AISettings,
        prompt: String,
        systemMessage: String?,
        maxTokens: Int,
        temperature: Float
    ): String {
        // OpenRouter uses similar API to OpenAI
        val messages = buildMessages(prompt, systemMessage)
        val request = OpenAIRequest(
            model = settings.model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature
        )

        // Use OpenAI service with OpenRouter base URL
        val response = openAIService.generateCompletion(
            authorization = "Bearer ${settings.apiKey}",
            request = request
        )

        return handleOpenAIResponse(response)
    }

    private suspend fun executeGroq(
        settings: AISettings,
        prompt: String,
        systemMessage: String?,
        maxTokens: Int,
        temperature: Float
    ): String {
        // Groq uses OpenAI-compatible API
        val messages = buildMessages(prompt, systemMessage)
        val request = OpenAIRequest(
            model = settings.model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature
        )

        val response = openAIService.generateCompletion(
            authorization = "Bearer ${settings.apiKey}",
            request = request
        )

        return handleOpenAIResponse(response)
    }

    private suspend fun executeOllama(
        settings: AISettings,
        prompt: String,
        systemMessage: String?,
        maxTokens: Int,
        temperature: Float
    ): String {
        // Ollama runs locally, no API key needed
        val messages = buildMessages(prompt, systemMessage)
        val request = OpenAIRequest(
            model = settings.model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature
        )

        val response = openAIService.generateCompletion(
            authorization = "Bearer ollama", // Ollama doesn't require auth
            request = request
        )

        return handleOpenAIResponse(response)
    }

    private suspend fun executeCustom(
        settings: AISettings,
        prompt: String,
        systemMessage: String?,
        maxTokens: Int,
        temperature: Float
    ): String {
        // Custom provider - assume OpenAI-compatible
        val messages = buildMessages(prompt, systemMessage)
        val request = OpenAIRequest(
            model = settings.model,
            messages = messages,
            max_tokens = maxTokens,
            temperature = temperature
        )

        val response = openAIService.generateCompletion(
            authorization = "Bearer ${settings.apiKey}",
            request = request
        )

        return handleOpenAIResponse(response)
    }

    private fun buildMessages(prompt: String, systemMessage: String?): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        if (systemMessage != null) {
            messages.add(ChatMessage("system", systemMessage))
        }

        messages.add(ChatMessage("user", prompt))
        return messages
    }

    private fun handleOpenAIResponse(response: Response<OpenAIResponse>): String {
        if (!response.isSuccessful) {
            throw Exception("API call failed: ${response.code()} ${response.message()}")
        }

        val body = response.body()
        if (body == null) {
            throw Exception("Empty response from API")
        }

        if (body.choices.isEmpty()) {
            throw Exception("No choices in response")
        }

        return body.choices.first().message.content
    }

    private fun getFallbackProvider(primary: AIProvider): AIProvider? {
        return when (primary) {
            AIProvider.OPENAI -> AIProvider.OPENROUTER
            AIProvider.OPENROUTER -> AIProvider.GROQ
            AIProvider.GROQ -> AIProvider.OLLAMA
            AIProvider.OLLAMA -> null // No fallback for local
            AIProvider.CUSTOM -> null // No fallback for custom
        }
    }

    /**
     * Validates provider configuration
     */
    suspend fun validateProvider(settings: AISettings): Result<String> = safeExecuteAsync {
        // Simple validation - try a basic prompt
        val testResult = executeWithProvider(settings, "Hello", null, 10, 0.1f)
        if (testResult.isSuccess) {
            "Provider ${settings.provider} is working correctly"
        } else {
            throw testResult.exceptionOrNull() ?: Exception("Unknown validation error")
        }
    }

    /**
     * Gets estimated cost for a request
     */
    fun estimateCost(provider: AIProvider, model: String, tokens: Int): Double {
        // Rough estimates per 1K tokens
        val costPerThousand = when (provider) {
            AIProvider.OPENAI -> when {
                model.contains("gpt-4") -> 0.03 // GPT-4
                else -> 0.002 // GPT-3.5
            }
            AIProvider.OPENROUTER -> 0.001 // Generally cheaper
            AIProvider.GROQ -> 0.0005 // Very cheap
            AIProvider.OLLAMA -> 0.0 // Free (local)
            AIProvider.CUSTOM -> 0.0 // Unknown
        }

        return (tokens / 1000.0) * costPerThousand
    }
}