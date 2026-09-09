package com.raven.data.model.repository

import com.raven.data.model.db.ModelDao
import com.raven.data.model.db.ModelEntity
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.repository.ModelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of ModelRepository using Room DAO.
 */
class RoomModelRepository(
    private val modelDao: ModelDao
) : ModelRepository {
    
    override suspend fun saveModel(model: ModelMetadata) {
        val entity = ModelEntity.fromDomain(model)
        modelDao.insertModel(entity)
    }
    
    override suspend fun getModelById(modelId: ModelId): ModelMetadata? {
        return modelDao.getModelById(modelId.value)?.toDomain()
    }
    
    override suspend fun getAllModels(): List<ModelMetadata> {
        return modelDao.getAllModels().map { it.toDomain() }
    }
    
    override fun observeAllModels(): Flow<List<ModelMetadata>> {
        return modelDao.observeAllModels().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun deleteModel(modelId: ModelId) {
        modelDao.deleteModelById(modelId.value)
    }
    
    override suspend fun deleteAllModels() {
        modelDao.deleteAllModels()
    }
    
    override suspend fun modelExists(modelId: ModelId): Boolean {
        return modelDao.modelExists(modelId.value)
    }
    
    override suspend fun updateLastUsed(modelId: ModelId) {
        modelDao.updateLastUsed(modelId.value)
    }
    
    override suspend fun countModels(): Int {
        return modelDao.countModels()
    }
}
