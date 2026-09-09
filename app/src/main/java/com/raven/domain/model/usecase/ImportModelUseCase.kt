package com.raven.domain.model.usecase

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.ModelSource
import com.raven.domain.model.ModelStatus
import com.raven.data.model.storage.ModelStorage
import com.raven.data.model.validation.GgufValidator
import com.raven.domain.model.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Orchestrates model import:
 * 1. Copy file to staging
 * 2. Validate file (GGUF format, checksum)
 * 3. Extract metadata
 * 4. Check device suitability
 * 5. Check storage availability
 * 6. Move from staging to final location
 * 7. Register in database
 *
 * If any step fails, clean up staging files and report error.
 */
class ImportModelUseCase(
    private val modelStorage: ModelStorage,
    private val validator: GgufValidator,
    private val modelRepository: ModelRepository,
) {
    
    suspend fun importModel(
        sourceFile: File,
        expectedSha256: String? = null,
        onProgress: suspend (ModelStatus) -> Unit = {}
    ): ModelMetadata = withContext(Dispatchers.IO) {
        val operationId = UUID.randomUUID().toString()
        var stagingFile: File? = null
        var finalizedModelId: ModelId? = null
        
        try {
            // Check storage before starting
            val requiredBytes = sourceFile.length()
            val availableBytes = modelStorage.availableStorageBytes()
            if (requiredBytes > availableBytes) {
                throw ModelError.InsufficientStorage(requiredBytes, availableBytes)
            }
            
            // STAGE 1: Copy to staging
            onProgress(ModelStatus.Importing)
            stagingFile = modelStorage.copyToStaging(sourceFile, operationId)
            
            // STAGE 2: Validate file existence
            onProgress(ModelStatus.Verifying)
            validator.validateFileExists(stagingFile)
            
            // STAGE 3: Validate GGUF format
            validator.validateGgufFormat(stagingFile)
            
            // STAGE 4: Validate checksum if provided
            if (expectedSha256 != null) {
                validator.validateChecksum(stagingFile, expectedSha256)
            }
            
            // Calculate actual checksum
            val actualSha256 = modelStorage.calculateSha256(stagingFile)
            val modelId = ModelId(
                "${sourceFile.nameWithoutExtension.sanitizedId()}-${actualSha256.take(12)}"
            )
            
            // STAGE 5: Extract metadata
            val capabilities = validator.extractMetadata(stagingFile)
            
            // STAGE 6: Check device suitability
            val deviceSuitability = validator.estimateDeviceSuitability(
                capabilities,
                stagingFile.length()
            )
            
            // Note: We don't block on device suitability - just warn/inform
            // User can proceed even with Caution/NotRecommended
            
            // STAGE 7: Move to final location
            val finalFile = modelStorage.moveFromStagingToFinal(stagingFile, modelId)
            finalizedModelId = modelId
            stagingFile = null // No longer in staging, don't clean up on error
            
            // STAGE 8: Create metadata
            val metadata = ModelMetadata(
                id = modelId,
                displayName = sourceFile.nameWithoutExtension,
                fileName = sourceFile.name,
                fileSizeBytes = finalFile.length(),
                status = ModelStatus.Installed,
                source = ModelSource.IMPORTED,
                capabilities = capabilities,
                sha256 = actualSha256,
                expectedSha256 = expectedSha256,
            )
            
            // STAGE 9: Register in database
            modelRepository.saveModel(metadata)
            
            onProgress(ModelStatus.Installed)
            return@withContext metadata
            
        } catch (e: ModelError) {
            // Clean up staging file on any model error
            if (stagingFile != null) {
                try {
                    modelStorage.deleteStagingFile(stagingFile)
                } catch (cleanupError: Exception) {
                    // Ignore cleanup errors, rethrow original
                }
            }
            finalizedModelId?.let { modelStorage.deleteModel(it) }
            throw e
        } catch (e: Exception) {
            // Clean up staging file on unexpected error
            if (stagingFile != null) {
                try {
                    modelStorage.deleteStagingFile(stagingFile)
                } catch (cleanupError: Exception) {
                    // Ignore cleanup errors
                }
            }
            finalizedModelId?.let { modelStorage.deleteModel(it) }
            throw ModelError.ImportFailed("Unexpected error: ${e.message}", e)
        }
    }

    private fun String.sanitizedId(): String {
        return lowercase()
            .replace(Regex("[^a-z0-9._-]"), "-")
            .trim('-')
            .take(40)
            .ifBlank { "model" }
    }
}
