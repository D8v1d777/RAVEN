package com.raven.data.model.storage

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import java.io.File

/**
 * Resolves paths for model storage operations.
 * Centralizes all path logic so it doesn't scatter throughout the codebase.
 * All paths are app-private and stable across app instances.
 */
interface ModelPathResolver {
    /**
     * Root directory where all models are stored.
     * Typically: context.filesDir/models/ or context.cacheDir/models/
     */
    fun getModelsRootDir(): File
    
    /** Directory for model registry (registry.json) */
    fun getRegistryDir(): File = File(getModelsRootDir(), "registry")
    
    /** Registry file location */
    fun getRegistryFile(): File = File(getRegistryDir(), "registry.json")
    
    /** Staging directory for in-progress downloads/imports */
    fun getStagingDir(): File = File(getModelsRootDir(), ".staging")
    
    /** Directory for a specific model's files */
    fun getModelDir(modelId: ModelId): File = File(getModelsRootDir(), modelId.value)
    
    /** The actual model file (GGUF) path */
    fun getModelFile(modelId: ModelId): File = File(getModelDir(modelId), "model.gguf")
    
    /** Model metadata file */
    fun getModelMetadataFile(modelId: ModelId): File = File(getModelDir(modelId), "metadata.json")
    
    /** Staging file for a specific download/import operation */
    fun getStagingFile(operationId: String): File = File(getStagingDir(), operationId)
    
    /** Free space available for model storage in bytes */
    fun getAvailableStorage(): Long
    
    /** Total usable storage space in bytes */
    fun getTotalStorage(): Long
}
