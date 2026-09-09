package com.raven.inference.online

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import com.raven.inference.provider.LlmProvider
import com.raven.inference.provider.ProviderHealth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Stub online provider for now.
 * Real implementation (OpenAiCompatibleProvider) comes in M4.
 * Ensures provider abstraction works before adding actual remote calls.
 */
class StubOnlineProvider : LlmProvider {
    
    override val id: String = "online"
    override val displayName: String = "Online"
    
    private var selectedModel: ModelDescriptor? = null
    
    override suspend fun listModels(): Result<List<ModelDescriptor>> {
        // Stub - real implementation would query remote API
        return Result.success(emptyList())
    }
    
    override fun stream(request: GenerationRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Failed(
            code = "NOT_CONFIGURED",
            message = "Online provider not yet configured. M4 milestone will implement this."
        ))
    }
    
    override suspend fun health(): ProviderHealth {
        return ProviderHealth.Unavailable("Online provider not configured")
    }
    
    override suspend fun selectModel(modelId: String): Result<Unit> {
        return Result.failure(Exception("Online provider not configured"))
    }
    
    override suspend fun getSelectedModel(): ModelDescriptor? = selectedModel
    
    override suspend fun unload(): Result<Unit> {
        selectedModel = null
        return Result.success(Unit)
    }
}
