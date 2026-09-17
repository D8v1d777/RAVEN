package com.raven.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.raven.AppContainer
import com.raven.RavenApplication

/**
 * Thin Android wrapper around [ConversationEngine].
 * All decision logic lives in the engine so it can be unit tested without Android.
 */
class ChatViewModel(
    container: AppContainer,
) : ViewModel() {

    private val engine = ConversationEngine(
        router = container.providerRouter,
        scope = viewModelScope,
    )

    val state = engine.state

    fun send(text: String) = engine.send(text)

    fun stop() = engine.stop()

    fun retry() = engine.retry()

    fun dismissError() = engine.dismissError()

    fun startNewConversation() = engine.clearConversation()

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RavenApplication
                ChatViewModel(application.container)
            }
        }
    }
}