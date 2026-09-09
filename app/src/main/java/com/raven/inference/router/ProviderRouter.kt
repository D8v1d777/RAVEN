package com.raven.inference.router

import com.raven.inference.provider.LlmProvider
import com.raven.domain.model.ModelError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Routes generation requests to the appropriate provider.
 * Responsibilities:
 * - Select active provider (Local vs Online)
 * - Validate provider availability
 * - Switch providers without conversation loss
 * - Prevent silent provider fallback
 * - Surface provider errors
 */
class ProviderRouter {
    
    private var selectedProviderId: String = "local"
    private val providers = mutableMapOf<String, LlmProvider>()
    
    /**
     * Register a provider (local, openai, etc.).
     * Must be done before use.
     */
    fun registerProvider(provider: LlmProvider) {
        providers[provider.id] = provider
        if (providers.size == 1) {
            selectedProviderId = provider.id // Default to first
        }
    }
    
    /**
     * Select which provider to use.
     * @throws ModelError if provider doesn't exist
     */
    suspend fun selectProvider(providerId: String): Result<Unit> = withContext(Dispatchers.Default) {
        return@withContext try {
            if (!providers.containsKey(providerId)) {
                return@withContext Result.failure(
                    ModelError.UnsupportedModel("Provider not registered: $providerId")
                )
            }
            selectedProviderId = providerId
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get the currently selected provider.
     */
    fun getSelectedProvider(): LlmProvider? = providers[selectedProviderId]
    
    /**
     * Get a specific provider.
     */
    fun getProvider(providerId: String): LlmProvider? = providers[providerId]
    
    /**
     * List all registered providers.
     */
    fun getAvailableProviders(): List<LlmProvider> = providers.values.toList()
    
    /**
     * Get currently selected provider ID.
     */
    fun getSelectedProviderId(): String = selectedProviderId
    
    /**
     * Check health of selected provider.
     */
    suspend fun checkSelectedProviderHealth() = getSelectedProvider()?.health()
}
