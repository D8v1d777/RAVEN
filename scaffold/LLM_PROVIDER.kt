package com.raven.inference

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import kotlinx.coroutines.flow.Flow

interface LlmProvider {
    val id: String
    suspend fun listModels(): List<ModelDescriptor>
    fun stream(request: GenerationRequest): Flow<GenerationEvent>
    suspend fun health(): ProviderHealth
}

sealed interface ProviderHealth {
    data object Ready : ProviderHealth
    data class Unavailable(val reason: String) : ProviderHealth
}
