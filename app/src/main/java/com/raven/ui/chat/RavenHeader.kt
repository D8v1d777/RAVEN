package com.raven.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.ui.theme.RavenColors
import com.raven.ui.theme.RavenSerif

/**
 * Identity bar: who Raven is, what it is doing right now, and which model is answering.
 * The model chip is the entry point to model switching.
 */
@Composable
fun RavenHeader(
    status: String,
    modelLabel: String?,
    onModelClick: () -> Unit,
    onStartNewConversation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().background(RavenColors.SurfaceDefault)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RavenAvatarMark(size = 40.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Raven",
                    color = RavenColors.TextPrimary,
                    fontFamily = RavenSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 19.sp,
                )
                Text(
                    text = status,
                    color = RavenColors.TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                )
            }

            ModelChip(label = modelLabel, onClick = onModelClick)

            IconButton(
                onClick = onStartNewConversation,
                modifier = Modifier
                    .size(40.dp)
                    .semantics { contentDescription = "New conversation" },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = RavenColors.TextSecondary,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RavenColors.Divider)
        )
    }
}

/** Three-tap model switching starts here: tap the chip, pick a model, confirm. */
@Composable
private fun ModelChip(label: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    val hasModel = label != null

    Row(
        modifier = Modifier
            .background(RavenColors.SurfaceElevated, shape)
            .border(
                width = 1.dp,
                color = if (hasModel) RavenColors.Primary.copy(alpha = 0.45f) else RavenColors.Divider,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .semantics { contentDescription = "Current model: ${label ?: "none"}. Change model." },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = if (hasModel) RavenColors.Success else RavenColors.Warning,
                    shape = CircleShape,
                )
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = label?.take(18) ?: "No model",
            color = if (hasModel) RavenColors.TextPrimary else RavenColors.TextSecondary,
            fontSize = 11.5.sp,
            maxLines = 1,
        )
    }
}

/** Neutral notice: used for model operations that happen outside a reply. */
@Composable
fun RavenNoticeBanner(
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(RavenColors.SurfaceElevated, RoundedCornerShape(14.dp))
            .border(1.dp, RavenColors.Divider, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            color = RavenColors.TextSecondary,
            fontSize = 12.5.sp,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = RavenColors.SecondaryAccent, fontSize = 12.5.sp)
            }
        }
        TextButton(onClick = onDismiss) {
            Text("Dismiss", color = RavenColors.TextTertiary, fontSize = 12.5.sp)
        }
    }
}