package com.raven.inference.local

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import com.raven.domain.model.ModelMetadata
import com.raven.inference.provider.LlmProvider
import com.raven.inference.provider.ProviderHealth
import com.raven.domain.model.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Local LLM provider using llama.cpp (or mock).
 * Responsible for:
 * - Model lifecycle (load/unload)
 * - Streaming token generation
 * - Cancellation and error handling
 * - Memory safety
 *
 * This class owns the native runtime boundary.
 * Native implementation details do NOT leak into domain/UI code.
 */
class LocalLlmProvider(
    private val modelRepository: ModelRepository,
    private val nativeRuntime: LocalInferenceRuntime,
) : LlmProvider {
    
    override val id: String = "local"
    override val displayName: String = "Local (Offline)"
    
    private var currentlyLoadedModelId: String? = null
    private var isLoading = false
    
    override suspend fun listModels(): Result<List<ModelDescriptor>> = withContext(Dispatchers.Default) {
        return@withContext try {
            val models = modelRepository.getAllModels()
            val descriptors = models.map { metadata ->
                ModelDescriptor(
                    id = metadata.id.value,
                    displayName = metadata.displayName,
                    providerId = id,
                    sizeBytes = metadata.fileSizeBytes,
                    format = "GGUF",
                    quantization = metadata.capabilities.quantization,
                )
            }
            Result.success(descriptors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun stream(request: GenerationRequest): Flow<GenerationEvent> = flow {
        try {
            // Verify model is loaded
            if (currentlyLoadedModelId != request.modelId) {
                selectModel(request.modelId).getOrThrow()
            }
            
            emit(GenerationEvent.Started)
            nativeRuntime.beginGeneration(request)

            var outputTokens = 0
            while (outputTokens < request.maxOutputTokens) {
                val token = nativeRuntime.generateToken() ?: break
                if (token.isNotEmpty()) {
                    emit(GenerationEvent.Token(token))
                    outputTokens++
                }
            }
            
            emit(GenerationEvent.Completed(
                inputTokens = request.messages.size,
                outputTokens = outputTokens
            ))
        } catch (e: kotlinx.coroutines.CancellationException) {
            nativeRuntime.cancelGeneration()
            emit(GenerationEvent.Cancelled)
            throw e
        } catch (e: Exception) {
            emit(GenerationEvent.Failed(
                code = "LOCAL_INFERENCE_ERROR",
                message = e.message ?: "Unknown error"
            ))
        }
    }
    
    override suspend fun health(): ProviderHealth = withContext(Dispatchers.Default) {
        return@withContext try {
            val models = modelRepository.getAllModels()
            if (models.isEmpty()) {
                ProviderHealth.Unavailable("No models installed")
            } else {
                ProviderHealth.Ready
            }
        } catch (e: Exception) {
            ProviderHealth.Unavailable("Repository error: ${e.message}")
        }
    }
    
    override suspend fun selectModel(modelId: String): Result<Unit> = withContext(Dispatchers.Default) {
        return@withContext try {
            if (isLoading) {
                return@withContext Result.failure(Exception("Model loading already in progress"))
            }
            
            if (currentlyLoadedModelId == modelId) {
                return@withContext Result.success(Unit)
            }
            
            isLoading = true
            try {
                // Unload previous model
                if (currentlyLoadedModelId != null) {
                    nativeRuntime.unloadModel()
                }
                
                // Get model metadata
                val model = modelRepository.getModelById(
                    com.raven.domain.model.ModelId(modelId)
                ) ?: return@withContext Result.failure(Exception("Model not found: $modelId"))
                
                // Load new model
                nativeRuntime.loadModel(model)
                currentlyLoadedModelId = modelId
                
                Result.success(Unit)
            } finally {
                isLoading = false
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSelectedModel(): ModelDescriptor? {
        if (currentlyLoadedModelId == null) return null
        return try {
            val metadata = modelRepository.getModelById(
                com.raven.domain.model.ModelId(currentlyLoadedModelId!!)
            ) ?: return null
            ModelDescriptor(
                id = metadata.id.value,
                displayName = metadata.displayName,
                providerId = id,
                sizeBytes = metadata.fileSizeBytes,
                format = "GGUF",
                quantization = metadata.capabilities.quantization,
            )
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun unload(): Result<Unit> = withContext(Dispatchers.Default) {
        return@withContext try {
            nativeRuntime.unloadModel()
            currentlyLoadedModelId = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
}
