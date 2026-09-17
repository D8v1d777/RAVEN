package com.raven.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.chat.ChatMessageUi
import com.raven.chat.MessageStatus
import com.raven.domain.Role
import com.raven.ui.rememberReducedMotion
import com.raven.ui.theme.RavenColors

/**
 * A single conversation message.
 *
 * Raven's replies are matte near-black cards on the left, the user's are violet-tinted on
 * the right. Streaming replies carry a live caret, and stopped or failed replies keep the
 * partial text instead of discarding it.
 */
@Composable
fun RavenMessageBubble(message: ChatMessageUi, modifier: Modifier = Modifier) {
    val isUser = message.role == Role.USER
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .background(
                    color = if (isUser) RavenColors.UserBubble else RavenColors.AssistantBubble,
                    shape = shape,
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) UserBubbleBorder else AssistantBubbleBorder,
                    shape = shape,
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row {
                SelectionContainer {
                    Text(
                        text = message.text,
                        color = RavenColors.TextPrimary,
                        fontSize = 15.5.sp,
                        lineHeight = 23.sp,
                    )
                }
                if (message.status == MessageStatus.STREAMING) {
                    StreamingCaret()
                }
            }
        }

        when {
            message.status == MessageStatus.CANCELLED ->
                MessageFootnote("Stopped \u2022 partial reply kept")

            message.status == MessageStatus.FAILED ->
                MessageFootnote("Failed \u2022 partial reply kept", color = RavenColors.ErrorText)

            !isUser && message.status == MessageStatus.COMPLETE &&
                message.outputTokenCount != null ->
                MessageFootnote("${message.outputTokenCount} tokens")
        }
    }
}

private val UserBubbleBorder = Color(0x559B4DCA)
private val AssistantBubbleBorder = Color(0x1AFFFFFF)

@Composable
private fun MessageFootnote(text: String, color: Color = RavenColors.TextTertiary) {
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 4.dp),
    )
}

/** Italic placeholder text used inside system notes. */
@Composable
internal fun RavenNote(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontStyle = FontStyle.Italic,
        modifier = modifier,
    )
}

/**
 * The caret at the end of a streaming reply. Honest motion: it animates only while tokens
 * are arriving, and it stops entirely when the user has reduced motion enabled.
 */
@Composable
private fun StreamingCaret() {
    val reducedMotion = rememberReducedMotion()
    val alpha = if (reducedMotion) {
        1f
    } else {
        rememberInfiniteTransition(label = "caret").animateFloat(
            initialValue = 0.15f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "caretAlpha",
        ).value
    }

    Text(
        text = "\u258C",
        color = RavenColors.SecondaryAccent.copy(alpha = alpha),
        fontSize = 15.5.sp,
        modifier = Modifier.padding(start = 2.dp),
    )
}

/**
 * Shown between "send" and the first token, while the model is still processing the
 * prompt. It reflects a real in-flight generation, not a decorative delay.
 */
@Composable
fun RavenWaitingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .background(RavenColors.AssistantBubble, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Raven is thinking",
                    color = RavenColors.TextSecondary,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                )
                Spacer(modifier = Modifier.width(8.dp))
                PulsingDot(delayMillis = 0)
                PulsingDot(delayMillis = 160)
                PulsingDot(delayMillis = 320)
            }
        }
    }
}

@Composable
private fun PulsingDot(delayMillis: Int) {
    val reducedMotion = rememberReducedMotion()
    val alpha = if (reducedMotion) {
        0.7f
    } else {
        rememberInfiniteTransition(label = "dot").animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 900,
                    delayMillis = delayMillis,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dotAlpha",
        ).value
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(5.dp)
            .alpha(alpha)
            .background(RavenColors.SecondaryAccent, CircleShape)
    )
}