package com.raven

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.raven.ui.chat.RavenChatRoute
import com.raven.ui.theme.RavenTheme

/**
 * Raven has a single entry point: the conversation.
 *
 * All dependencies are assembled once in [RavenApplication]; the Activity only hosts the
 * Compose tree, so it contains no model, runtime or provider wiring.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RavenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    RavenChatRoute()
                }
            }
        }
    }
}
