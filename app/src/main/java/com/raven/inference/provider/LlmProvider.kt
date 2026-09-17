package com.raven.inference.provider

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
     * True when inference runs entirely on this device and no conversation data
     * leaves it. Privacy UI relies on this: an online provider must never be
     * presented as local.
     */
    val runsOnDevice: Boolean

    /**
     * Reactive view of the currently loaded model.
     * Null means the provider cannot generate yet.
     */
    val selectedModel: StateFlow<ModelDescriptor?>
    
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
     * Release the provider's conversation session so the next request starts clean.
     * Local providers drop the llama.cpp KV cache and chat history.
     */
    suspend fun resetSession(): Result<Unit> = Result.success(Unit)

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
