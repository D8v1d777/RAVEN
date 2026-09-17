package com.raven.chat

import com.raven.domain.Role

/** What the conversation layer is currently doing. */
enum class ChatPhase {
    /** Nothing in flight; the user can send. */
    IDLE,

    /** A generation is streaming tokens. */
    GENERATING,
}

/** Lifecycle of a single rendered message. */
enum class MessageStatus {
    COMPLETE,

    /** Tokens are still arriving. */
    STREAMING,

    /** The user stopped generation; partial text is preserved. */
    CANCELLED,

    /** Generation failed; partial text is preserved and retry is offered. */
    FAILED,
}

data class ChatMessageUi(
    val id: String,
    val role: Role,
    val text: String,
    val status: MessageStatus,
    val createdAtEpochMs: Long,
    val modelId: String? = null,
    val outputTokenCount: Int? = null,
)

/**
 * A user-facing failure with a stable code and a real explanation.
 * Nothing here is a generic "something went wrong".
 */
data class ChatError(
    val code: String,
    val message: String,
    val retryable: Boolean,
)

data class ChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val phase: ChatPhase = ChatPhase.IDLE,
    val providerId: String? = null,
    val modelId: String? = null,
    val modelLabel: String? = null,
    val runsOnDevice: Boolean = true,
    val lastError: ChatError? = null,
) {
    val isGenerating: Boolean get() = phase == ChatPhase.GENERATING

    val canSend: Boolean get() = phase == ChatPhase.IDLE

    val hasUserMessage: Boolean get() = messages.any { it.role == Role.USER }

    val canRetry: Boolean get() = phase == ChatPhase.IDLE && lastError?.retryable == true && hasUserMessage
}