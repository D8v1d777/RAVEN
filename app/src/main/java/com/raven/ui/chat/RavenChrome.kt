package com.raven.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.chat.ChatError
import com.raven.ui.theme.RavenColors
import com.raven.ui.theme.RavenSerif

/** Explicit failure surface: the real reason plus Retry when retrying can actually help. */
@Composable
fun RavenErrorBanner(
    error: ChatError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(RavenColors.ErrorContainer, RoundedCornerShape(16.dp))
            .border(1.dp, RavenColors.ErrorText.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = error.message,
            color = RavenColors.ErrorText,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        Text(
            text = error.code,
            color = RavenColors.TextTertiary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = RavenColors.TextSecondary, fontSize = 13.sp)
            }
            if (error.retryable) {
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onRetry) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = RavenColors.SecondaryAccent,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retry", color = RavenColors.SecondaryAccent, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Shown when the conversation is empty. It offers exactly one next step, chosen from what
 * is actually true about the installed models.
 */
@Composable
fun RavenEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RavenAvatarMark(size = 76.dp)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = title,
            color = RavenColors.TextPrimary,
            fontFamily = RavenSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 26.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        RavenNote(
            text = subtitle,
            color = RavenColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(onClick = onAction) {
            Text(
                text = actionLabel,
                color = RavenColors.SecondaryAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/**
 * Raven's identity mark: a violet-lit obsidian plate carrying a serif R.
 * This is replaced by the canonical artwork once it is present in the repository.
 */
@Composable
fun RavenAvatarMark(size: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size / 3)

    Box(
        modifier = modifier
            .size(size)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(RavenColors.SurfaceElevated, RavenColors.Background),
                ),
                shape = shape,
            )
            .border(width = 1.dp, color = RavenColors.Primary.copy(alpha = 0.5f), shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size / 1.6f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(RavenColors.AccentGlow, Color.Transparent),
                    ),
                    shape = CircleShape,
                )
        )
        Text(
            text = "R",
            color = RavenColors.SecondaryAccent,
            fontFamily = RavenSerif,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value / 2.1f).sp,
        )
    }
}