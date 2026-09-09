package com.raven.data.model.storage

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Implementation of ModelStorage for actual filesystem operations.
 * All operations run on IO dispatcher to avoid blocking UI.
 */
class FileModelStorage(
    private val pathResolver: ModelPathResolver
) : ModelStorage {

    override suspend fun availableStorageBytes(): Long = withContext(Dispatchers.IO) {
        pathResolver.getAvailableStorage()
    }
    
    override suspend fun copyToStaging(
        sourceFile: File,
        stagingFileName: String
    ): File = withContext(Dispatchers.IO) {
        if (stagingFileName.isBlank() || stagingFileName != File(stagingFileName).name) {
            throw ModelError.CopyFailed("Invalid staging file name")
        }
        if (!sourceFile.exists()) {
            throw ModelError.CopyFailed("Source file does not exist: ${sourceFile.absolutePath}")
        }
        if (!sourceFile.isFile) {
            throw ModelError.CopyFailed("Source is not a file: ${sourceFile.absolutePath}")
        }
        
        val stagingDir = pathResolver.getStagingDir()
        stagingDir.mkdirs()
        
        val targetFile = File(stagingDir, stagingFileName)
        
        try {
            sourceFile.copyTo(targetFile, overwrite = false)
            return@withContext targetFile
        } catch (e: Exception) {
            // Clean up if copy fails
            targetFile.delete()
            throw ModelError.CopyFailed("Failed to copy file: ${e.message}", e)
        }
    }
    
    override suspend fun moveFromStagingToFinal(
        stagingFile: File,
        modelId: ModelId
    ): File = withContext(Dispatchers.IO) {
        if (!stagingFile.exists()) {
            throw ModelError.CopyFailed("Staging file does not exist: ${stagingFile.absolutePath}")
        }
        
        val modelDir = pathResolver.getModelDir(modelId)
        val modelsRoot = pathResolver.getModelsRootDir().canonicalFile
        val modelPath = modelDir.canonicalPath
        val rootPath = modelsRoot.canonicalPath
        val normalizedModelPath = if (modelPath.endsWith(File.separator)) modelPath else modelPath + File.separator
        val normalizedRootPath = if (rootPath.endsWith(File.separator)) rootPath else rootPath + File.separator
        if (modelPath != rootPath && !normalizedModelPath.startsWith(normalizedRootPath)) {
            throw ModelError.CopyFailed("Model path escapes managed storage")
        }
        modelDir.mkdirs()
        
        val finalFile = pathResolver.getModelFile(modelId)
        if (finalFile.exists()) {
            throw ModelError.CopyFailed("Model destination already exists")
        }
        
        try {
            stagingFile.renameTo(finalFile)
            if (!finalFile.exists()) {
                throw Exception("Rename operation reported success but file doesn't exist")
            }
            return@withContext finalFile
        } catch (e: Exception) {
            throw ModelError.CopyFailed("Failed to move file to final location: ${e.message}", e)
        }
    }
    
    override suspend fun deleteModel(modelId: ModelId) = withContext(Dispatchers.IO) {
        val modelDir = pathResolver.getModelDir(modelId)
        if (modelDir.exists()) {
            modelDir.deleteRecursively()
        }
    }
    
    override suspend fun deleteStagingFile(stagingFile: File) = withContext(Dispatchers.IO) {
        if (stagingFile.exists()) {
            stagingFile.delete()
        }
    }
    
    override suspend fun modelExists(modelId: ModelId): Boolean = withContext(Dispatchers.IO) {
        val modelFile = pathResolver.getModelFile(modelId)
        modelFile.exists() && modelFile.isFile
    }
    
    override suspend fun getModelFile(modelId: ModelId): File? = withContext(Dispatchers.IO) {
        val modelFile = pathResolver.getModelFile(modelId)
        if (modelFile.exists() && modelFile.isFile) {
            modelFile
        } else {
            null
        }
    }
    
    override suspend fun calculateSha256(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw ModelError.InvalidFile("File does not exist: ${file.absolutePath}")
        }

        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return@withContext digest.digest().toList().joinToString(separator = "") { "%02x".format(it) }
    }
    
    override suspend fun listModels(): List<File> = withContext(Dispatchers.IO) {
        val modelsRoot = pathResolver.getModelsRootDir()
        if (!modelsRoot.exists()) return@withContext emptyList()
        
        modelsRoot.listFiles { file ->
            file.isDirectory && file.name != "registry" && file.name != ".staging"
        }?.toList() ?: emptyList()
    }
    
    override suspend fun cleanupStagingDirectory() = withContext(Dispatchers.IO) {
        val stagingDir = pathResolver.getStagingDir()
        if (!stagingDir.exists()) return@withContext
        
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
        stagingDir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }
}
