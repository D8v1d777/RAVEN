package com.raven.inference.local

import com.raven.domain.model.ModelMetadata

/**
 * Mock implementation of LocalInferenceRuntime for testing.
 * Doesn't require native libraries or actual model files.
 * Perfect for unit tests and early development.
 */
class MockLocalInferenceRuntime : LocalInferenceRuntime {
    
    private var loadedModel: ModelMetadata? = null
    private var generationState = GenerationState()
    
    private data class GenerationState(
        val tokens: MutableList<String> = mutableListOf(),
        var currentTokenIndex: Int = 0
    ) {
        fun reset() {
            tokens.clear()
            currentTokenIndex = 0
        }
    }
    
    override suspend fun loadModel(metadata: ModelMetadata) {
        if (loadedModel != null) {
            unloadModel()
        }
        loadedModel = metadata
        generationState.reset()
    }
    
    override suspend fun unloadModel() {
        loadedModel = null
        generationState.reset()
    }
    
    override fun isModelLoaded(): Boolean = loadedModel != null
    
    override fun getCurrentModel(): ModelMetadata? = loadedModel
    
    override suspend fun generateToken(): String? {
        val model = loadedModel ?: return null
        
        // Mock generation: return a simple response
        if (generationState.tokens.isEmpty()) {
            val mockResponse = buildMockResponse(model)
            generationState.tokens.addAll(mockResponse.split(" "))
        }
        
        return if (generationState.currentTokenIndex < generationState.tokens.size) {
            generationState.tokens[generationState.currentTokenIndex++] + " "
        } else {
            null // Generation complete
        }
    }
    
    private fun buildMockResponse(metadata: ModelMetadata): String {
        return """
            This is a mock response from ${metadata.displayName}. 
            Real llama.cpp integration will replace this. 
            The model is loaded but not actually generating tokens yet. 
            Next step: integrate native llama.cpp runtime.
        """.trimIndent()
    }
}
