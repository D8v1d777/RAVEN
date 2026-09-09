package com.raven.data.model.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raven.domain.model.ModelCapabilities
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.ModelSource
import com.raven.domain.model.ModelStatus
import kotlinx.serialization.Serializable

/**
 * Room entity for persisting model metadata.
 * Stored in the local database, survives app restarts.
 */
@Entity(tableName = "models")
@Serializable
data class ModelEntity(
    @PrimaryKey
    val id: String,
    
    val displayName: String,
    val fileName: String,
    val fileSizeBytes: Long,
    
    // Serialized status + source
    val status: String, // JSON serialized ModelStatus
    val source: String, // IMPORTED, DOWNLOADED, etc.
    
    // Capabilities (serialized as JSON)
    val architecture: String? = null,
    val quantization: String? = null,
    val parameterCount: Long? = null,
    val contextLength: Int? = null,
    val embeddingDim: Int? = null,
    val vocabularySize: Int? = null,
    val modelFamily: String? = null,
    val trainingDataCutoff: String? = null,
    val estimatedRamRequirementBytes: Long? = null,
    
    // Integrity/identity
    val sha256: String? = null,
    val expectedSha256: String? = null,
    
    // Timestamps
    val installedAtEpochMs: Long = System.currentTimeMillis(),
    val lastUsedAtEpochMs: Long? = null,
    
    // License + source
    val licenseText: String? = null,
    val sourceUrl: String? = null,
    
    // Provider ownership
    val providerId: String = "local",
) {
    fun toDomain(): ModelMetadata {
        return ModelMetadata(
            id = ModelId(id),
            displayName = displayName,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            status = parseStatus(status),
            source = ModelSource.fromString(source),
            capabilities = ModelCapabilities(
                architecture = architecture,
                quantization = quantization,
                parameterCount = parameterCount,
                contextLength = contextLength,
                embeddingDim = embeddingDim,
                vocabularySize = vocabularySize,
                modelFamily = modelFamily,
                trainingDataCutoff = trainingDataCutoff,
                estimatedRamRequirementBytes = estimatedRamRequirementBytes,
            ),
            sha256 = sha256,
            expectedSha256 = expectedSha256,
            installedAtEpochMs = installedAtEpochMs,
            lastUsedAtEpochMs = lastUsedAtEpochMs,
            licenseText = licenseText,
            sourceUrl = sourceUrl,
            providerId = providerId,
        )
    }
    
    companion object {
        fun fromDomain(metadata: ModelMetadata): ModelEntity {
            return ModelEntity(
                id = metadata.id.value,
                displayName = metadata.displayName,
                fileName = metadata.fileName,
                fileSizeBytes = metadata.fileSizeBytes,
                status = serializeStatus(metadata.status),
                source = metadata.source.name,
                architecture = metadata.capabilities.architecture,
                quantization = metadata.capabilities.quantization,
                parameterCount = metadata.capabilities.parameterCount,
                contextLength = metadata.capabilities.contextLength,
                embeddingDim = metadata.capabilities.embeddingDim,
                vocabularySize = metadata.capabilities.vocabularySize,
                modelFamily = metadata.capabilities.modelFamily,
                trainingDataCutoff = metadata.capabilities.trainingDataCutoff,
                estimatedRamRequirementBytes = metadata.capabilities.estimatedRamRequirementBytes,
                sha256 = metadata.sha256,
                expectedSha256 = metadata.expectedSha256,
                installedAtEpochMs = metadata.installedAtEpochMs,
                lastUsedAtEpochMs = metadata.lastUsedAtEpochMs,
                licenseText = metadata.licenseText,
                sourceUrl = metadata.sourceUrl,
                providerId = metadata.providerId,
            )
        }
        
        private fun serializeStatus(status: ModelStatus): String {
            return when (status) {
                ModelStatus.Discovered -> "Discovered"
                is ModelStatus.Downloading -> "Downloading:${status.progressBytes}:${status.totalBytes}"
                ModelStatus.Importing -> "Importing"
                ModelStatus.Verifying -> "Verifying"
                ModelStatus.Installed -> "Installed"
                ModelStatus.Ready -> "Ready"
                ModelStatus.Loading -> "Loading"
                ModelStatus.Loaded -> "Loaded"
                is ModelStatus.DownloadFailed -> "DownloadFailed:${status.reason.encodeStatusValue()}"
                is ModelStatus.ImportFailed -> "ImportFailed:${status.reason.encodeStatusValue()}"
                is ModelStatus.ValidationFailed -> "ValidationFailed:${status.reason.encodeStatusValue()}"
                is ModelStatus.InvalidFormat -> "InvalidFormat:${status.reason.encodeStatusValue()}"
                is ModelStatus.ChecksumFailed -> "ChecksumFailed:${status.expected.orEmpty().encodeStatusValue()}:${status.actual.encodeStatusValue()}"
                is ModelStatus.UnsupportedModel -> "UnsupportedModel:${status.reason.encodeStatusValue()}"
                is ModelStatus.InsufficientStorage -> "InsufficientStorage:${status.requiredBytes}:${status.availableBytes}"
                is ModelStatus.InsufficientMemory -> "InsufficientMemory:${status.requiredBytes}:${status.availableBytes}"
                is ModelStatus.LoadFailed -> "LoadFailed:${status.reason.encodeStatusValue()}"
            }
        }
        
        private fun parseStatus(status: String): ModelStatus {
            val parts = status.split(":", limit = 3)
            return when (parts.firstOrNull()) {
                "Discovered" -> ModelStatus.Discovered
                "Downloading" -> parts.toLongPairOr(ModelStatus.Discovered) { progress, total ->
                    ModelStatus.Downloading(progress, total)
                }
                "Importing" -> ModelStatus.Importing
                "Verifying" -> ModelStatus.Verifying
                "Installed" -> ModelStatus.Installed
                "Ready" -> ModelStatus.Ready
                "Loading" -> ModelStatus.Loading
                "Loaded" -> ModelStatus.Loaded
                "DownloadFailed" -> ModelStatus.DownloadFailed(parts.getOrDecoded(1))
                "ImportFailed" -> ModelStatus.ImportFailed(parts.getOrDecoded(1))
                "ValidationFailed" -> ModelStatus.ValidationFailed(parts.getOrDecoded(1))
                "InvalidFormat" -> ModelStatus.InvalidFormat(parts.getOrDecoded(1))
                "ChecksumFailed" -> ModelStatus.ChecksumFailed(
                    parts.getOrDecoded(1).ifBlank { null },
                    parts.getOrDecoded(2)
                )
                "UnsupportedModel" -> ModelStatus.UnsupportedModel(parts.getOrDecoded(1))
                "InsufficientStorage" -> parts.toLongPairOr(ModelStatus.Discovered) { required, available ->
                    ModelStatus.InsufficientStorage(required, available)
                }
                "InsufficientMemory" -> parts.toLongPairOr(ModelStatus.Discovered) { required, available ->
                    ModelStatus.InsufficientMemory(required, available)
                }
                "LoadFailed" -> ModelStatus.LoadFailed(parts.getOrDecoded(1))
                else -> ModelStatus.Discovered
            }
        }

        private fun String.encodeStatusValue(): String =
            replace("%", "%25").replace(":", "%3A")

        private fun List<String>.getOrDecoded(index: Int): String =
            getOrNull(index).orEmpty()
                .replace("%3A", ":")
                .replace("%25", "%")

        private fun List<String>.toLongPairOr(
            fallback: ModelStatus,
            factory: (Long, Long) -> ModelStatus
        ): ModelStatus {
            val first = getOrNull(1)?.toLongOrNull() ?: return fallback
            val second = getOrNull(2)?.toLongOrNull() ?: return fallback
            return factory(first, second)
        }
    }
}
