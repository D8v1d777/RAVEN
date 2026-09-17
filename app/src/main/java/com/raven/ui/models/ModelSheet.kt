package com.raven.ui.models

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raven.domain.model.ModelMetadata
import com.raven.models.ModelCatalogState
import com.raven.ui.theme.RavenColors
import com.raven.ui.theme.RavenSerif

/**
 * The model hub.
 *
 * Everything here reflects real storage: which GGUF files are installed, which one is in
 * memory, and how much space they occupy. Loading performs an actual llama.cpp load.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    state: ModelCatalogState,
    onDismiss: () -> Unit,
    onLoadModel: (String) -> Unit,
    onUnloadModel: () -> Unit,
    onDeleteModel: (String) -> Unit,
    onImportUri: (Uri) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImportUri(uri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = RavenColors.SurfaceDefault,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Models",
                color = RavenColors.TextPrimary,
                fontFamily = RavenSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 2.dp),
            )
            Text(
                text = storageLine(state),
                color = RavenColors.TextTertiary,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            )

            if (state.isWorking) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = RavenColors.SecondaryAccent,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = state.workLabel ?: "Working\u2026",
                        color = RavenColors.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }

            state.warningMessage?.let { warning ->
                Text(
                    text = warning,
                    color = RavenColors.Warning,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (state.models.isEmpty()) {
                Text(
                    text = "No GGUF models installed yet.",
                    color = RavenColors.TextSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    items(items = state.models, key = { it.id.value }) { model ->
                        ModelRow(
                            model = model,
                            isLoaded = model.id.value == state.loadedModelId,
                            isBusy = state.isWorking,
                            onLoad = { onLoadModel(model.id.value) },
                            onUnload = onUnloadModel,
                            onDelete = { onDeleteModel(model.id.value) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { picker.launch("*/*") }, enabled = !state.isWorking) {
                    Text("Import GGUF", color = RavenColors.SecondaryAccent, fontSize = 13.sp)
                }
                if (state.loadedModelId != null) {
                    TextButton(onClick = onUnloadModel, enabled = !state.isWorking) {
                        Text("Unload", color = RavenColors.TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            Text(
                text = "Models are stored in private app storage and are never uploaded.",
                color = RavenColors.TextTertiary,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * One installed model. The row shows what is genuinely known about the file: size from the
 * filesystem, and architecture/quantisation/context when GGUF metadata extraction provides
 * them. Unknown values are shown as unknown rather than invented.
 */
@Composable
private fun ModelRow(
    model: ModelMetadata,
    isLoaded: Boolean,
    isBusy: Boolean,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDelete: () -> Unit,
) {
    val capabilities = model.capabilities

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(RavenColors.SurfaceElevated, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isLoaded) RavenColors.Success else RavenColors.TextTertiary,
                        shape = CircleShape,
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = model.displayName,
                color = RavenColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatBytes(model.fileSizeBytes),
                color = RavenColors.TextSecondary,
                fontSize = 12.sp,
            )
        }

        Text(
            text = descriptorLine(model),
            color = RavenColors.TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 18.dp, top = 4.dp),
        )

        Row(
            modifier = Modifier.padding(start = 10.dp, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoaded) {
                TextButton(onClick = onUnload, enabled = !isBusy) {
                    Text("Unload", color = RavenColors.TextSecondary, fontSize = 12.5.sp)
                }
            } else {
                TextButton(onClick = onLoad, enabled = !isBusy) {
                    Text("Load", color = RavenColors.SecondaryAccent, fontSize = 12.5.sp)
                }
            }
            TextButton(onClick = onDelete, enabled = !isBusy) {
                Text("Delete", color = RavenColors.ErrorText, fontSize = 12.5.sp)
            }
            if (!capabilities.hasMetadata()) {
                Text(
                    text = "metadata not extracted yet",
                    color = RavenColors.TextTertiary,
                    fontSize = 10.5.sp,
                )
            }
        }
    }
}

private fun descriptorLine(model: ModelMetadata): String {
    val capabilities = model.capabilities
    val parts = mutableListOf<String>()
    capabilities.architecture?.let { parts.add(it) }
    capabilities.quantization?.let { parts.add(it) }
    capabilities.contextLength?.let { parts.add("${it} ctx") }
    capabilities.parameterCount?.let { parts.add("${it}M params") }
    parts.add("GGUF")
    return parts.joinToString(" \u2022 ")
}

private fun storageLine(state: ModelCatalogState): String {
    val used = formatBytes(state.storageUsedBytes)
    return if (state.storageAvailableBytes > 0) {
        "$used used \u2022 ${formatBytes(state.storageAvailableBytes)} free"
    } else {
        "$used used"
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024L -> String.format("%.0f MB", bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> String.format("%.0f KB", bytes / 1024.0)
    else -> "$bytes B"
}