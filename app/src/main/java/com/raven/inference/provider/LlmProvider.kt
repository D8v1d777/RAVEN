package com.raven.inference.provider

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import kotlinx.coroutines.flow.Flow

/**
 * Contract for language model providers (local or remote).
 * Enforces provider independence - same interface for all providers.
 * UI never imports provider-specific implementations.
 */
interface LlmProvider {
    /** Stable provider identifier */
    val id: String
    
    /** Human-readable provider name */
    val displayName: String
    
    /**
     * List available models for this provider.
     * For local providers: installed models
     * For remote providers: models at the configured endpoint
     */
    suspend fun listModels(): Result<List<ModelDescriptor>>
    
    /**
     * Generate tokens for a request.
     * Returns a Flow of generation events for streaming.
     * Must be cancellable via coroutine cancellation.
     * @param request the generation request
     * @return Flow of GenerationEvent
     */
    fun stream(request: GenerationRequest): Flow<GenerationEvent>
    
    /**
     * Check provider health/availability.
     * @return ProviderHealth status
     */
    suspend fun health(): ProviderHealth
    
    /**
     * Select/load a specific model.
     * @param modelId the model to select
     * @return success or error
     */
    suspend fun selectModel(modelId: String): Result<Unit>
    
    /**
     * Get currently selected model.
     */
    suspend fun getSelectedModel(): ModelDescriptor?
    
    /**
     * Unload/release the current model.
     * Should free resources on local providers.
     */
    suspend fun unload(): Result<Unit>
}

sealed interface ProviderHealth {
    data object Ready : ProviderHealth
    data class Unavailable(val reason: String) : ProviderHealth
    data class Degraded(val reason: String) : ProviderHealth
}
