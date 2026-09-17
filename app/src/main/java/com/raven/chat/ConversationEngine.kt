package com.raven.chat

import com.raven.domain.ChatMessage
import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.Role
import com.raven.domain.model.ModelError
import com.raven.inference.router.ProviderRouter
import com.raven.persona.RavenPersona
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the conversation: messages, generation lifecycle, cancellation and retry.
 *
 * Sits between the UI and the provider layer exactly as docs/ARCHITECTURE.md requires:
 *
 *   UI -> ViewModel -> ConversationEngine -> ProviderRouter -> LlmProvider -> runtime
 *
 * This class contains no Android dependencies, which keeps the whole loop unit testable.
 * It never fabricates a response: every token rendered in the UI came from a provider.
 */
class ConversationEngine(
    private val router: ProviderRouter,
    private val scope: CoroutineScope,
    private val persona: RavenPersona = RavenPersona,
    private val temperature: Float = 0.75f,
    private val topP: Float = 0.95f,
    private val maxOutputTokens: Int = 512,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** Full conversation history, newest last. Used to build each GenerationRequest. */
    private val history = mutableListOf<ChatMessage>()

    private var generationJob: Job? = null

    /**
     * Sends a user message and starts streaming a response.
     * Blank input is ignored outright: no empty message is ever added to the conversation.
     */
    fun send(rawText: String) {
        val text = rawText.trim()
        if (text.isBlank()) return
        if (generationJob?.isActive == true) return

        history.add(
            ChatMessage(
                id = newId(),
                role = Role.USER,
                content = text,
                createdAtEpochMs = now(),
            )
        )

        _state.update { current ->
            current.copy(
                messages = current.messages + ChatMessageUi(
                    id = newId(),
                    role = Role.USER,
                    text = text,
                    status = MessageStatus.COMPLETE,
                    createdAtEpochMs = now(),
                ),
                lastError = null,
            )
        }

        startGeneration()
    }

    /**
     * Re-runs the last user turn after a retryable failure.
     * The failed assistant message is dropped so retries do not stack up.
     */
    fun retry() {
        if (generationJob?.isActive == true) return
        if (_state.value.messages.none { it.role == Role.USER }) return

        _state.update { current ->
            current.copy(
                messages = current.messages.dropLastWhile {
                    it.role == Role.ASSISTANT && it.status == MessageStatus.FAILED
                },
                lastError = null,
            )
        }

        startGeneration()
    }

    /**
     * Cancels the active generation. Cancellation propagates into the provider flow,
     * which reaches the native llama.cpp cancellation flag. Partial text is preserved
     * and the message is marked as stopped.
     */
    fun stop() {
        val job = generationJob ?: return
        if (!job.isActive) return
        job.cancel()
    }

    fun dismissError() {
        _state.update { it.copy(lastError = null) }
    }

    /**
     * Starts an empty conversation and resets the provider's inference session, so the
     * model genuinely does not remember the previous conversation.
     */
    fun clearConversation() {
        if (generationJob?.isActive == true) stop()
        history.clear()
        _state.update { ChatUiState() }
        scope.launch {
            runCatching { router.getSelectedProvider()?.resetSession() }
        }
    }

    /**
     * Streams one assistant response for the last user message in [history].
     *
     * Failure modes are explicit:
     * - no provider registered -> PROVIDER_UNAVAILABLE
     * - no model loaded        -> MODEL_NOT_LOADED
     * - provider failure       -> the provider's own stable code
     */
    private fun startGeneration() {
        val assistantMessageId = newId()
        val assistantCreatedAt = now()
        _state.update { current ->
            current.copy(
                phase = ChatPhase.GENERATING,
                lastError = null,
                messages = current.messages + ChatMessageUi(
                    id = assistantMessageId,
                    role = Role.ASSISTANT,
                    text = "",
                    status = MessageStatus.STREAMING,
                    createdAtEpochMs = assistantCreatedAt,
                ),
            )
        }

        val job = scope.launch {
            val partial = StringBuilder()
            var streamedTokens = 0

            try {
                val provider = router.getSelectedProvider()
                    ?: throw ChatFailure(
                        ChatError(
                            code = "PROVIDER_UNAVAILABLE",
                            message = "No inference provider is registered.",
                            retryable = false,
                        )
                    )

                val selected = provider.selectedModel.value
                    ?: throw ChatFailure(
                        ChatError(
                            code = "MODEL_NOT_LOADED",
                            message = "No model is loaded. Choose one in the model hub first.",
                            retryable = false,
                        )
                    )

                _state.update { current ->
                    current.copy(
                        providerId = provider.id,
                        runsOnDevice = provider.runsOnDevice,
                        modelId = selected.id,
                        modelLabel = selected.displayName,
                        messages = current.messages.map { message ->
                            if (message.id == assistantMessageId) {
                                message.copy(
                                    status = MessageStatus.STREAMING,
                                    modelId = selected.id,
                                )
                            } else {
                                message
                            }
                        },
                    )
                }

                val request = GenerationRequest(
                    modelId = selected.id,
                    systemPrompt = persona.systemPrompt(),
                    messages = history.toList(),
                    temperature = temperature,
                    topP = topP,
                    maxOutputTokens = maxOutputTokens,
                )

                provider.stream(request).collect { event ->
                    when (event) {
                        is GenerationEvent.Started -> Unit

                        is GenerationEvent.Token -> {
                            partial.append(event.text)
                            streamedTokens++
                            val text = partial.toString()
                            updateMessage(assistantMessageId) {
                                it.copy(text = text, outputTokenCount = streamedTokens)
                            }
                        }

                        is GenerationEvent.Completed -> {
                            updateMessage(assistantMessageId) {
                                it.copy(
                                    status = MessageStatus.COMPLETE,
                                    outputTokenCount = event.outputTokens ?: streamedTokens,
                                )
                            }
                            recordAssistantTurn(partial.toString())
                        }

                        is GenerationEvent.Cancelled -> Unit

                        is GenerationEvent.Failed -> throw ChatFailure(
                            ChatError(
                                code = event.code,
                                message = event.message,
                                retryable = true,
                            )
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                // Keep the partial answer, but report it as stopped rather than complete.
                updateMessage(assistantMessageId) { it.copy(status = MessageStatus.CANCELLED) }
                throw cancellation
            } catch (error: Exception) {
                updateMessage(assistantMessageId) { it.copy(status = MessageStatus.FAILED) }
                _state.update { it.copy(lastError = error.toChatError()) }
            } finally {
                _state.update { it.copy(phase = ChatPhase.IDLE) }
            }
        }

        generationJob = job
        job.invokeOnCompletion {
            if (generationJob === job) generationJob = null
        }
    }

    private fun updateMessage(id: String, transform: (ChatMessageUi) -> ChatMessageUi) {
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == id) transform(message) else message
                }
            )
        }
    }

    /**
     * Only completed answers enter [history]. A stopped or failed turn never reached the
     * model's own session, so recording it would desynchronise the prompt history.
     */
    private fun recordAssistantTurn(text: String) {
        if (text.isBlank()) return
        history.add(
            ChatMessage(
                id = newId(),
                role = Role.ASSISTANT,
                content = text,
                createdAtEpochMs = now(),
            )
        )
    }
}

/** Internal carrier so provider failures keep their stable code and message. */
private class ChatFailure(val error: ChatError) : Exception(error.message)

private fun Throwable.toChatError(): ChatError = when (this) {
    is ChatFailure -> error

    is ModelError.NotFound -> ChatError(
        code = "MODEL_NOT_FOUND",
        message = "The selected model is no longer installed.",
        retryable = false,
    )

    is ModelError.ModelLoadFailed -> ChatError(
        code = "MODEL_LOAD_FAILED",
        message = message ?: "The model could not be loaded.",
        retryable = true,
    )

    is ModelError.InsufficientMemory -> ChatError(
        code = "OUT_OF_MEMORY_RISK",
        message = "Not enough free memory to run this model. Close other apps and try again.",
        retryable = true,
    )

    is ModelError.InsufficientStorage -> ChatError(
        code = "INSUFFICIENT_STORAGE",
        message = "Not enough storage for this operation.",
        retryable = false,
    )

    is ModelError.UnsupportedModel -> ChatError(
        code = "MODEL_UNSUPPORTED",
        message = message ?: "This model cannot be used on this device.",
        retryable = false,
    )

    is ModelError.InvalidFile,
    is ModelError.ValidationFailed,
    is ModelError.ChecksumMismatch -> ChatError(
        code = "INVALID_MODEL_FILE",
        message = "The model file is not usable. Re-import it and try again.",
        retryable = false,
    )

    else -> ChatError(
        code = "GENERATION_FAILED",
        message = message ?: "Generation failed.",
        retryable = true,
    )
}