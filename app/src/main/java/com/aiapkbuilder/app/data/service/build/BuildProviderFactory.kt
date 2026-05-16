package com.aiapkbuilder.app.data.service.build

import com.aiapkbuilder.app.data.model.BuildProvider
import com.aiapkbuilder.app.data.service.build.provider.CodemagicProvider
import com.aiapkbuilder.app.data.service.build.provider.DockerProvider
import com.aiapkbuilder.app.data.service.build.provider.GitHubActionsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating build provider instances.
 * Handles provider instantiation and health checking.
 */
@Singleton
class BuildProviderFactory @Inject constructor(
    private val githubActionsProvider: GitHubActionsProvider,
    private val codemagicProvider: CodemagicProvider,
    private val dockerProvider: DockerProvider
) {

    /**
     * Gets the appropriate provider for the given build provider type.
     * @param provider The build provider type
     * @return IBuildProvider instance
     */
    fun getProvider(provider: BuildProvider): IBuildProvider {
        return when (provider) {
            BuildProvider.GITHUB_ACTIONS -> githubActionsProvider
            BuildProvider.CODEMAGIC -> codemagicProvider
            BuildProvider.DOCKER -> dockerProvider
            BuildProvider.SELF_HOSTED -> TODO("Self-hosted provider not implemented")
            BuildProvider.COMMUNITY -> TODO("Community provider not implemented")
            BuildProvider.LOCAL -> TODO("Local provider should use Docker")
        }
    }

    /**
     * Gets all available providers sorted by preference.
     * @return List of providers in order of preference
     */
    fun getAvailableProviders(): List<IBuildProvider> = listOf(
        githubActionsProvider,
        codemagicProvider,
        dockerProvider
    )

    /**
     * Gets healthy providers only.
     * @return List of healthy providers
     */
    suspend fun getHealthyProviders(): List<IBuildProvider> {
        return getAvailableProviders().filter { it.isHealthy() }
    }

    /**
     * Selects the best available provider based on health and cost.
     * @return Best available provider, or null if none available
     */
    suspend fun selectBestProvider(): IBuildProvider? {
        val healthyProviders = getHealthyProviders()
        if (healthyProviders.isEmpty()) return null

        // For now, prefer GitHub Actions, then Codemagic, then Docker
        return healthyProviders.firstOrNull()
    }
}