package com.raven.domain

data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val providerId: String,
    val sizeBytes: Long? = null,
    val format: String? = null,
    val quantization: String? = null,
)

data class GenerationRequest(
    val modelId: String,
    val systemPrompt: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.8f,
    val topP: Float = 0.95f,
    val maxOutputTokens: Int = 512,
)

data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val createdAtEpochMs: Long,
)

enum class Role { SYSTEM, USER, ASSISTANT }

sealed interface GenerationEvent {
    data object Started : GenerationEvent
    data class Token(val text: String) : GenerationEvent
    data class Completed(val inputTokens: Int? = null, val outputTokens: Int? = null) : GenerationEvent
    data object Cancelled : GenerationEvent
    data class Failed(val code: String, val message: String) : GenerationEvent
}
