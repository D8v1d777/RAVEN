package com.raven.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.raven.ui.theme.RavenColors

/**
 * The composer. The primary action is Send while idle and Stop while generating, so
 * stopping is always one deliberate tap away.
 */
@Composable
fun RavenComposer(
    input: String,
    onInputChange: (String) -> Unit,
    canSend: Boolean,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .semantics { contentDescription = "Message Raven" },
            placeholder = {
                Text("Talk to me\u2026", color = RavenColors.TextTertiary)
            },
            maxLines = 5,
            shape = shape,
            enabled = !isGenerating,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (canSend && input.isNotBlank()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSend()
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RavenColors.TextPrimary,
                unfocusedTextColor = RavenColors.TextPrimary,
                focusedContainerColor = RavenColors.SurfaceDefault,
                unfocusedContainerColor = RavenColors.SurfaceDefault,
                disabledContainerColor = RavenColors.SurfaceDefault,
                focusedBorderColor = RavenColors.Primary,
                unfocusedBorderColor = RavenColors.Divider,
                disabledBorderColor = RavenColors.Divider,
                cursorColor = RavenColors.SecondaryAccent,
            ),
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (isGenerating) {
            StopButton(onStop)
        } else {
            SendButton(
                enabled = canSend && input.isNotBlank(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSend()
                },
            )
        }
    }
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val container = if (enabled) RavenColors.Primary else RavenColors.SurfaceVariant

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(52.dp)
            .background(container, RoundedCornerShape(18.dp))
            .semantics { contentDescription = "Send message" },
    ) {
        Icon(
            imageVector = Icons.Filled.Send,
            contentDescription = null,
            tint = if (enabled) Color.White else RavenColors.TextTertiary,
        )
    }
}

/**
 * Stop is drawn rather than iconified: a solid square reads instantly as "cancel this",
 * and it does not depend on an icon that is missing from the core icon set.
 */
@Composable
private fun StopButton(onStop: () -> Unit) {
    IconButton(
        onClick = onStop,
        modifier = Modifier
            .size(52.dp)
            .background(RavenColors.MutedCrimson, RoundedCornerShape(18.dp))
            .semantics { contentDescription = "Stop generating" },
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.White, RoundedCornerShape(3.dp))
        )
    }
}