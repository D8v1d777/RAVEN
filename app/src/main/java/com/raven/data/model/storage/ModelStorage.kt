package com.raven.data.model.storage

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import java.io.File

/**
 * Abstracts model file operations.
 * Handles copying, moving, deleting model files safely.
 * All operations are coroutine-friendly and work off-main-thread.
 */
interface ModelStorage {
    /** Return available bytes in the app-private model volume. */
    suspend fun availableStorageBytes(): Long

    /**
     * Copy a file to the staging directory.
     * @param sourceFile the source file (from user's file picker or download)
     * @param stagingFileName name for the staged file
     * @return the path to the staged file
     * @throws ModelError.CopyFailed if copy fails
     */
    suspend fun copyToStaging(
        sourceFile: File,
        stagingFileName: String
    ): File
    
    /**
     * Move a file from staging to its final model directory.
     * @param stagingFile the file in staging
     * @param modelId the target model ID
     * @return the final model file path
     * @throws ModelError.CopyFailed if move fails
     */
    suspend fun moveFromStagingToFinal(
        stagingFile: File,
        modelId: ModelId
    ): File
    
    /**
     * Delete a model completely (all files).
     * Safe to call even if model is partially deleted.
     * @param modelId the model to delete
     */
    suspend fun deleteModel(modelId: ModelId)
    
    /**
     * Delete a staging file (for cleanup after failed imports).
     * @param stagingFile the file to delete
     */
    suspend fun deleteStagingFile(stagingFile: File)
    
    /**
     * Check if a model directory exists and contains the model file.
     */
    suspend fun modelExists(modelId: ModelId): Boolean
    
    /**
     * Get the model file if it exists.
     * @return the model file, or null if doesn't exist
     */
    suspend fun getModelFile(modelId: ModelId): File?
    
    /**
     * Calculate SHA-256 hash of a file.
     * Heavy operation, use on background thread.
     * @return hex string of SHA-256
     */
    suspend fun calculateSha256(file: File): String
    
    /**
     * List all models currently in storage.
     * @return list of model directories
     */
    suspend fun listModels(): List<File>
    
    /**
     * Clean up old staging files (older than 1 hour, configurable).
     */
    suspend fun cleanupStagingDirectory()
}
