package com.raven.domain.model.repository

import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for model operations.
 * Abstracts the model persistence layer (Room).
 * Used by domain/use case layer.
 */
interface ModelRepository {
    /**
     * Save a model's metadata to registry.
     */
    suspend fun saveModel(model: ModelMetadata)
    
    /**
     * Get a model by ID.
     */
    suspend fun getModelById(modelId: ModelId): ModelMetadata?
    
    /**
     * Get all installed models.
     */
    suspend fun getAllModels(): List<ModelMetadata>
    
    /**
     * Observe all models (reactive).
     */
    fun observeAllModels(): Flow<List<ModelMetadata>>
    
    /**
     * Delete a model from registry.
     */
    suspend fun deleteModel(modelId: ModelId)
    
    /**
     * Delete all models from registry.
     */
    suspend fun deleteAllModels()
    
    /**
     * Check if a model exists in registry.
     */
    suspend fun modelExists(modelId: ModelId): Boolean
    
    /**
     * Update model's last used timestamp.
     */
    suspend fun updateLastUsed(modelId: ModelId)
    
    /**
     * Get count of models.
     */
    suspend fun countModels(): Int
}
