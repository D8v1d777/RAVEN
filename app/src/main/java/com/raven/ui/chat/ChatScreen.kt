package com.raven.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.raven.chat.ChatUiState
import com.raven.chat.MessageStatus
import com.raven.models.ModelCatalogState
import com.raven.ui.rememberReducedMotion
import com.raven.ui.theme.RavenColors

/**
 * The conversation surface.
 *
 * State-driven and side-effect free: it renders what the engine and the model catalog
 * actually report, and forwards user intent. No fake tokens, no fake status.
 */
@Composable
fun ChatScreen(
    chatState: ChatUiState,
    catalogState: ModelCatalogState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDismissChatError: () -> Unit,
    onOpenModels: () -> Unit,
    onDismissModelNotice: () -> Unit,
    onStartNewConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val reducedMotion = rememberReducedMotion()

    val messages = chatState.messages
    val lastIndex = messages.lastIndex

    // Follow the stream. Instant scrolling while tokens arrive keeps the frame budget free;
    // a smooth scroll is only used when a whole new message appears.
    LaunchedEffect(lastIndex, messages.lastOrNull()?.text?.length) {
        if (lastIndex >= 0) {
            val streaming = messages.lastOrNull()?.status == MessageStatus.STREAMING
            if (streaming || reducedMotion) {
                listState.scrollToItem(lastIndex)
            } else {
                listState.animateScrollToItem(lastIndex)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(RavenColors.SurfaceDefault, RavenColors.Background),
                )
            )
            .imePadding(),
    ) {
        RavenHeader(
            status = statusFor(chatState, catalogState),
            modelLabel = chatState.modelLabel ?: catalogState.loadedModelLabel,
            onModelClick = onOpenModels,
            onStartNewConversation = onStartNewConversation,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            if (messages.isEmpty()) {
                RavenEmptyState(
                    title = emptyTitle(catalogState),
                    subtitle = emptySubtitle(catalogState),
                    actionLabel = emptyAction(catalogState),
                    onAction = onOpenModels,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                ) {
                    items(items = messages, key = { it.id }) { message ->
                        RavenMessageBubble(message)
                    }

                    if (chatState.isGenerating && messages.lastOrNull()?.text.isNullOrEmpty()) {
                        item(key = "waiting") { RavenWaitingIndicator() }
                    }
                }
            }
        }

        chatState.lastError?.let { error ->
            RavenErrorBanner(
                error = error,
                onRetry = onRetry,
                onDismiss = onDismissChatError,
            )
        }

        catalogState.errorMessage?.let { message ->
            RavenNoticeBanner(
                message = message,
                actionLabel = "Models",
                onAction = onOpenModels,
                onDismiss = onDismissModelNotice,
            )
        }

        RavenComposer(
            input = input,
            onInputChange = { input = it },
            canSend = chatState.canSend,
            isGenerating = chatState.isGenerating,
            onSend = {
                onSend(input)
                input = ""
            },
            onStop = onStop,
        )
    }
}

/** What the header says, derived from real state rather than a hard-coded label. */
private fun statusFor(chatState: ChatUiState, catalogState: ModelCatalogState): String = when {
    catalogState.isWorking -> catalogState.workLabel ?: "Working\u2026"
    chatState.isGenerating -> "Generating\u2026"
    chatState.modelLabel != null -> if (chatState.runsOnDevice) {
        "On device \u2022 ${chatState.modelLabel}"
    } else {
        "Online \u2022 ${chatState.modelLabel}"
    }
    catalogState.hasModels -> "Model installed but not loaded"
    else -> "No model installed"
}

private fun emptyTitle(catalogState: ModelCatalogState): String =
    if (catalogState.hasModels) "I'm here." else "Raven is listening."

private fun emptySubtitle(catalogState: ModelCatalogState): String = when {
    catalogState.isWorking -> catalogState.workLabel ?: "Preparing\u2026"
    catalogState.hasModels -> "Load a model and talk to me."
    else -> "Import a GGUF model from this device to begin. Nothing you type ever leaves it."
}

private fun emptyAction(catalogState: ModelCatalogState): String =
    if (catalogState.hasModels) "Choose model" else "Import a model"