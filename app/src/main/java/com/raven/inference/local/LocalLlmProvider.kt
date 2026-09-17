package com.raven.inference.local

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.repository.ModelRepository
import com.raven.inference.provider.LlmProvider
import com.raven.inference.provider.ProviderHealth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Local LLM provider using llama.cpp.
 * Responsible for:
 * - Model lifecycle (load/unload/switch with rollback)
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
    
    override val id: String = ID
    override val displayName: String = "Local (Offline)"
    override val runsOnDevice: Boolean = true

    /**
     * Serialises every operation that touches the single native model slot.
     * Loading and generating concurrently would corrupt the llama.cpp context.
     */
    private val modelOperationMutex = Mutex()

    private val _selectedModel = MutableStateFlow<ModelDescriptor?>(null)
    override val selectedModel: StateFlow<ModelDescriptor?> = _selectedModel.asStateFlow()
    
    override suspend fun listModels(): Result<List<ModelDescriptor>> = withContext(Dispatchers.Default) {
        return@withContext try {
            Result.success(modelRepository.getAllModels().map { it.toDescriptor() })
        } catch (e: Exception) {
            Result.failure(ModelError.ModelLoadFailed("Cannot read the model registry: ${e.message}", e))
        }
    }
    
    override fun stream(request: GenerationRequest): Flow<GenerationEvent> = flow {
        try {
            val loaded = _selectedModel.value
            if (loaded?.id != request.modelId) {
                selectModel(request.modelId).getOrElse { throw it }
            }

            emit(GenerationEvent.Started)
            nativeRuntime.beginGeneration(request)

            var outputTokens = 0
            var steps = 0
            val maxSteps = request.maxOutputTokens + MAX_PARTIAL_UTF8_STEPS

            // The runtime returns null when generation finished or was cancelled, and an
            // empty string while a multi-byte UTF-8 character is still being stitched.
            while (outputTokens < request.maxOutputTokens && steps < maxSteps) {
                steps++
                val token = nativeRuntime.generateToken() ?: break
                if (token.isEmpty()) continue
                outputTokens++
                emit(GenerationEvent.Token(token))
            }

            // inputTokens stays null: the local runtime does not report a prompt token
            // count yet, and inventing one would be a fake metric.
            emit(GenerationEvent.Completed(outputTokens = outputTokens))
        } catch (cancellation: CancellationException) {
            // Reaching the native cancellation path is mandatory, even though this
            // coroutine is already cancelled.
            withContext(NonCancellable) { nativeRuntime.cancelGeneration() }
            throw cancellation
        } catch (error: Exception) {
            emit(
                GenerationEvent.Failed(
                    code = error.generationCode(),
                    message = error.message ?: error.javaClass.simpleName,
                )
            )
        }
    }
    
    override suspend fun health(): ProviderHealth = withContext(Dispatchers.Default) {
        return@withContext try {
            if (_selectedModel.value != null) {
                ProviderHealth.Ready
            } else {
                val models = modelRepository.getAllModels()
                if (models.isEmpty()) {
                    ProviderHealth.Unavailable("No models installed")
                } else {
                    ProviderHealth.Degraded("${models.size} model(s) installed, none loaded")
                }
            }
        } catch (e: Exception) {
            ProviderHealth.Unavailable("Repository error: ${e.message}")
        }
    }
    
    override suspend fun selectModel(modelId: String): Result<Unit> = withContext(Dispatchers.Default) {
        val requestedId = parseModelId(modelId)
            ?: return@withContext Result.failure(
                ModelError.UnsupportedModel("Invalid model identifier: $modelId")
            )

        val metadata = try {
            modelRepository.getModelById(requestedId)
                ?: return@withContext Result.failure(ModelError.NotFound(requestedId))
        } catch (e: Exception) {
            return@withContext Result.failure(
                ModelError.ModelLoadFailed("Cannot read the model registry: ${e.message}", e)
            )
        }

        modelOperationMutex.withLock {
            if (_selectedModel.value?.id == metadata.id.value && nativeRuntime.isModelLoaded()) {
                return@withContext Result.success(Unit)
            }

            val previousMetadata = _selectedModel.value?.id?.let { previousId ->
                parseModelId(previousId)?.let { id ->
                    runCatching { modelRepository.getModelById(id) }.getOrNull()
                }
            }

            try {
                // The native runtime releases the previously loaded model itself.
                nativeRuntime.loadModel(metadata)
            } catch (error: Exception) {
                val failure = error.toModelError()
                restorePreviousModel(previousMetadata)
                return@withContext Result.failure(failure)
            }

            _selectedModel.value = metadata.toDescriptor()
            runCatching { modelRepository.updateLastUsed(metadata.id) }
            Result.success(Unit)
        }
    }

    override suspend fun getSelectedModel(): ModelDescriptor? = _selectedModel.value

    override suspend fun resetSession(): Result<Unit> = withContext(Dispatchers.Default) {
        modelOperationMutex.withLock {
            try {
                nativeRuntime.resetSession()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e.toModelError())
            }
        }
    }
    
    override suspend fun unload(): Result<Unit> = withContext(Dispatchers.Default) {
        modelOperationMutex.withLock {
            try {
                nativeRuntime.unloadModel()
                _selectedModel.value = null
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e.toModelError())
            }
        }
    }

    /**
     * Best-effort recovery after a failed model switch. The native runtime holds a
     * single model slot, so a failed load leaves nothing loaded; reload the model
     * that used to be active so the running conversation is not destroyed.
     */
    private suspend fun restorePreviousModel(previous: ModelMetadata?) {
        runCatching { nativeRuntime.unloadModel() }
        _selectedModel.value = null
        if (previous == null) return

        val restored = runCatching { nativeRuntime.loadModel(previous) }
        if (restored.isSuccess) {
            _selectedModel.value = previous.toDescriptor()
        }
    }

    companion object {
        const val ID = "local"

        /** Upper bound on the extra pulls used to stitch partial UTF-8 sequences. */
        private const val MAX_PARTIAL_UTF8_STEPS = 32
    }
}

internal fun ModelMetadata.toDescriptor(): ModelDescriptor = ModelDescriptor(
    id = id.value,
    displayName = displayName,
    providerId = providerId,
    sizeBytes = fileSizeBytes,
    format = "GGUF",
    quantization = capabilities.quantization,
)

/** Stable, UI-facing failure codes. See docs/ARCHITECTURE.md failure boundaries. */
internal fun Throwable.generationCode(): String = when (this) {
    is ModelError.NotFound -> "MODEL_NOT_FOUND"
    is ModelError.ModelLoadFailed -> "MODEL_LOAD_FAILED"
    is ModelError.InsufficientMemory -> "OUT_OF_MEMORY_RISK"
    is ModelError.InsufficientStorage -> "INSUFFICIENT_STORAGE"
    is ModelError.InvalidFile -> "INVALID_MODEL_FILE"
    is ModelError.ValidationFailed -> "INVALID_MODEL_FILE"
    is ModelError.ChecksumMismatch -> "INVALID_MODEL_FILE"
    is ModelError.UnsupportedModel -> "MODEL_UNSUPPORTED"
    else -> "GENERATION_FAILED"
}

internal fun Throwable.toModelError(): ModelError =
    this as? ModelError ?: ModelError.ModelLoadFailed(message ?: javaClass.simpleName, this)

private fun parseModelId(raw: String): ModelId? = try {
    ModelId(raw)
} catch (e: IllegalArgumentException) {
    null
}

