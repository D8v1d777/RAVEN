package com.raven.inference.local

import android.content.Context
import com.raven.data.model.storage.ModelPathResolver
import com.raven.domain.GenerationRequest
import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelMetadata
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Raven adapter for the upstream llama.cpp Android JNI lifecycle.
 * Native handles and llama.cpp types remain private to this class and JNI.
 */
class LlamaCppInferenceRuntime(
    context: Context,
    private val pathResolver: ModelPathResolver,
) : LocalInferenceRuntime {
    private val nativeLibraryDirectory = context.applicationInfo.nativeLibraryDir
    private val stateMutex = Mutex()
    private var initialized = false
    private var loadedModel: ModelMetadata? = null

    override suspend fun loadModel(metadata: ModelMetadata) = withContext(Dispatchers.Default) {
        stateMutex.withLock {
            val modelFile = pathResolver.getModelFile(metadata.id).canonicalFile
            if (!modelFile.isFile || !modelFile.canRead()) {
                throw ModelError.NotFound(metadata.id)
            }

            ensureInitialized()
            if (loadedModel != null) {
                nativeUnload()
                loadedModel = null
            }

            checkNativeResult(load(modelFile.absolutePath), "load model")
            try {
                checkNativeResult(prepare(), "prepare model")
                loadedModel = metadata
            } catch (error: Throwable) {
                nativeUnload()
                throw error
            }
        }
    }

    override suspend fun beginGeneration(request: GenerationRequest) = withContext(Dispatchers.Default) {
        stateMutex.withLock {
            if (loadedModel?.id?.value != request.modelId) {
                throw ModelError.NotFound(com.raven.domain.model.ModelId(request.modelId))
            }
            checkNativeResult(processSystemPrompt(request.systemPrompt), "process system prompt")
            val prompt = request.messages.lastOrNull { it.role == com.raven.domain.Role.USER }?.content
                ?: throw ModelError.GenerationFailed("Generation request has no user message")
            checkNativeResult(processUserPrompt(prompt, request.maxOutputTokens), "process user prompt")
        }
    }

    override suspend fun cancelGeneration() = withContext(Dispatchers.Default) {
        nativeCancelGeneration()
    }

    override suspend fun unloadModel() = withContext(Dispatchers.Default) {
        stateMutex.withLock {
            if (loadedModel != null) {
                nativeUnload()
                loadedModel = null
            }
        }
    }

    override fun isModelLoaded(): Boolean = loadedModel != null

    override fun getCurrentModel(): ModelMetadata? = loadedModel

    override suspend fun generateToken(): String? = withContext(Dispatchers.Default) {
        stateMutex.withLock { generateNextToken() }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            init(nativeLibraryDirectory)
            initialized = true
        }
    }

    private fun checkNativeResult(result: Int, operation: String) {
        if (result != 0) {
            throw ModelError.ModelLoadFailed("llama.cpp failed to $operation (code $result)")
        }
    }

    private external fun init(nativeLibraryDirectory: String)
    private external fun load(modelPath: String): Int
    private external fun prepare(): Int
    private external fun processSystemPrompt(systemPrompt: String): Int
    private external fun processUserPrompt(userPrompt: String, maxOutputTokens: Int): Int
    private external fun generateNextToken(): String?
    private external fun nativeCancelGeneration()
    private external fun nativeUnload()

    companion object {
        init {
            System.loadLibrary("raven_llama")
        }
    }
}
