package com.raven.data.model.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for model database operations.
 * All methods are suspend functions for coroutine compatibility.
 */
@Dao
interface ModelDao {
    /**
     * Insert a new model.
     * On conflict, replace the existing model.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity)
    
    /**
     * Update an existing model's metadata.
     */
    @Update
    suspend fun updateModel(model: ModelEntity)
    
    /**
     * Get a model by ID.
     */
    @Query("SELECT * FROM models WHERE id = :modelId LIMIT 1")
    suspend fun getModelById(modelId: String): ModelEntity?
    
    /**
     * Get all models, ordered by last used descending.
     * Models that have never been used appear last.
     */
    @Query("SELECT * FROM models ORDER BY CASE WHEN lastUsedAtEpochMs IS NULL THEN 1 ELSE 0 END, lastUsedAtEpochMs DESC")
    suspend fun getAllModels(): List<ModelEntity>

    /**
     * Observe all models as a Flow (for reactive UI).
     */
    @Query("SELECT * FROM models ORDER BY CASE WHEN lastUsedAtEpochMs IS NULL THEN 1 ELSE 0 END, lastUsedAtEpochMs DESC")
    fun observeAllModels(): Flow<List<ModelEntity>>
    
    /**
     * Delete a model by ID.
     */
    @Query("DELETE FROM models WHERE id = :modelId")
    suspend fun deleteModelById(modelId: String)
    
    /**
     * Delete all models.
     */
    @Query("DELETE FROM models")
    suspend fun deleteAllModels()
    
    /**
     * Check if a model exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM models WHERE id = :modelId)")
    suspend fun modelExists(modelId: String): Boolean
    
    /**
     * Count total models.
     */
    @Query("SELECT COUNT(*) FROM models")
    suspend fun countModels(): Int
    
    /**
     * Update a model's last used timestamp.
     */
    @Query("UPDATE models SET lastUsedAtEpochMs = :timestamp WHERE id = :modelId")
    suspend fun updateLastUsed(modelId: String, timestamp: Long = System.currentTimeMillis())
}
