package com.raven.inference.local

import com.raven.domain.model.ModelMetadata
import com.raven.domain.GenerationRequest

/**
 * Boundary interface for native llama.cpp runtime.
 * All native/JNI details are hidden behind this.
 *
 * Implementation options:
 * 1. MockLocalInferenceRuntime - for testing
 * 2. LlamaCppInferenceRuntime - actual llama.cpp integration (STEP 10)
 */
interface LocalInferenceRuntime {
    /** Prepare native state for one generation request. */
    suspend fun beginGeneration(request: GenerationRequest) = Unit

    /** Request cancellation of the active native generation. */
    suspend fun cancelGeneration() = Unit

    /**
     * Load a GGUF model into memory.
     * After this completes, the model is ready for generation.
     * @throws Exception if loading fails
     */
    suspend fun loadModel(metadata: ModelMetadata)
    
    /**
     * Unload the current model and free resources.
     */
    suspend fun unloadModel()
    
    /**
     * Check if a model is currently loaded.
     */
    fun isModelLoaded(): Boolean
    
    /**
     * Get currently loaded model metadata.
     */
    fun getCurrentModel(): ModelMetadata?
    
    /**
     * Generate a single token.
     * Returns the generated text token (not necessarily a word).
     * Must be called repeatedly to generate a full response.
     * Returns null if generation is complete.
     * @throws Exception if generation fails
     */
    suspend fun generateToken(): String?
}
