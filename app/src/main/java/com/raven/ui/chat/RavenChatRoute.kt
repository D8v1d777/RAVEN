package com.raven.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raven.chat.ChatViewModel
import com.raven.models.ModelCatalogViewModel
import com.raven.ui.models.ModelSheet

/**
 * Wires the chat and model ViewModels to the UI.
 *
 * This is the only place that knows about both, which keeps [ChatScreen] a pure function of
 * state and keeps model management out of the conversation layer.
 */
@Composable
fun RavenChatRoute(
    chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory),
    catalogViewModel: ModelCatalogViewModel = viewModel(factory = ModelCatalogViewModel.Factory),
) {
    val chatState by chatViewModel.state.collectAsState()
    val catalogState by catalogViewModel.state.collectAsState()
    var showModelSheet by remember { mutableStateOf(false) }

    // Restore the model the user was using last, once per process.
    LaunchedEffect(Unit) {
        catalogViewModel.restoreLastUsedModel()
    }

    ChatScreen(
        chatState = chatState,
        catalogState = catalogState,
        onSend = chatViewModel::send,
        onStop = chatViewModel::stop,
        onRetry = chatViewModel::retry,
        onDismissChatError = chatViewModel::dismissError,
        onOpenModels = { showModelSheet = true },
        onDismissModelNotice = catalogViewModel::dismissMessages,
        onStartNewConversation = chatViewModel::startNewConversation,
    )

    if (showModelSheet) {
        ModelSheet(
            state = catalogState,
            onDismiss = { showModelSheet = false },
            onLoadModel = catalogViewModel::loadModel,
            onUnloadModel = catalogViewModel::unloadModel,
            onDeleteModel = catalogViewModel::deleteModel,
            onImportUri = catalogViewModel::importModelFromUri,
        )
    }
}