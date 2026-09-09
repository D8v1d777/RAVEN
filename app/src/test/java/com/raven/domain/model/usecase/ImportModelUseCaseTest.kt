package com.raven.domain.model.usecase

import com.raven.data.model.storage.FileModelStorage
import com.raven.data.model.storage.ModelPathResolver
import com.raven.data.model.storage.ModelStorage
import com.raven.data.model.validation.DeviceSuitability
import com.raven.data.model.validation.GgufValidator
import com.raven.domain.model.ModelCapabilities
import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.repository.ModelRepository
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportModelUseCaseTest {
    private lateinit var root: File
    private lateinit var repository: RecordingRepository
    private lateinit var storage: FileModelStorage
    private lateinit var useCase: ImportModelUseCase

    @Before
    fun setUp() {
        root = Files.createTempDirectory("raven-import").toFile()
        storage = FileModelStorage(TestPathResolver(root))
        repository = RecordingRepository()
        useCase = ImportModelUseCase(storage, AcceptingValidator(storage), repository)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun validImportFinalizesFileAndRegistersInstalledModel() = runBlocking {
        val source = root.resolve("Raven Model.gguf").apply { writeBytes(ByteArray(2048) { 7 }) }

        val metadata = useCase.importModel(source)

        assertTrue(storage.modelExists(metadata.id))
        assertEquals(metadata.id, repository.saved.single().id)
        assertEquals(com.raven.domain.model.ModelStatus.Installed, metadata.status)
        assertTrue(storage.listModels().single().name.startsWith("raven-model-"))
        assertTrue(root.resolve("models/.staging").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun failedValidationDoesNotRegisterOrInstall() = runBlocking {
        val failingUseCase = ImportModelUseCase(storage, RejectingValidator(), repository)
        val source = root.resolve("invalid.gguf").apply { writeBytes(ByteArray(2048)) }

        try {
            failingUseCase.importModel(source)
            org.junit.Assert.fail("Expected validation failure")
        } catch (error: ModelError.InvalidFile) {
            assertTrue(error.message!!.contains("invalid"))
        }

        assertTrue(repository.saved.isEmpty())
        assertTrue(storage.listModels().isEmpty())
        assertTrue(root.resolve("models/.staging").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun insufficientStorageStopsBeforeCopy() = runBlocking {
        val constrainedStorage = FileModelStorage(TestPathResolver(root, availableBytes = 1L))
        val constrainedUseCase = ImportModelUseCase(
            constrainedStorage,
            AcceptingValidator(constrainedStorage),
            repository
        )
        val source = root.resolve("large.gguf").apply { writeBytes(ByteArray(2048)) }

        try {
            constrainedUseCase.importModel(source)
            org.junit.Assert.fail("Expected insufficient storage")
        } catch (error: ModelError.InsufficientStorage) {
            assertEquals(1L, error.availableBytes)
        }

        assertTrue(repository.saved.isEmpty())
        assertFalse(root.resolve("models/.staging").exists())
    }

    @Test
    fun destinationCollisionDoesNotDeleteExistingModel() = runBlocking {
        val source = root.resolve("same-name.gguf").apply { writeBytes(ByteArray(2048) { 3 }) }
        val first = useCase.importModel(source)
        val secondStorage = FileModelStorage(TestPathResolver(root))
        val secondUseCase = ImportModelUseCase(
            secondStorage,
            AcceptingValidator(secondStorage),
            RecordingRepository()
        )

        try {
            secondUseCase.importModel(source)
            org.junit.Assert.fail("Expected destination collision")
        } catch (_: ModelError.ImportFailed) {
        } catch (_: ModelError.CopyFailed) {
        }

        assertTrue(storage.modelExists(first.id))
        assertEquals("model.gguf", storage.getModelFile(first.id)?.name)
    }

    private class TestPathResolver(
        private val root: File,
        private val availableBytes: Long = 1024L * 1024L
    ) : ModelPathResolver {
        override fun getModelsRootDir() = root.resolve("models")
        override fun getAvailableStorage() = availableBytes
        override fun getTotalStorage() = 1024L * 1024L
    }

    private class AcceptingValidator(private val storage: ModelStorage) : GgufValidator {
        override suspend fun validateFileExists(file: File) = Unit
        override suspend fun validateChecksum(file: File, expectedSha256: String) = Unit
        override suspend fun validateGgufFormat(file: File) = Unit
        override suspend fun extractMetadata(file: File) = ModelCapabilities()
        override suspend fun estimateDeviceSuitability(
            capabilities: ModelCapabilities,
            fileSizeBytes: Long
        ) = DeviceSuitability.Recommended
    }

    private class RejectingValidator : GgufValidator {
        override suspend fun validateFileExists(file: File) =
            throw ModelError.InvalidFile("invalid fixture")
        override suspend fun validateChecksum(file: File, expectedSha256: String) = Unit
        override suspend fun validateGgufFormat(file: File) = Unit
        override suspend fun extractMetadata(file: File) = ModelCapabilities()
        override suspend fun estimateDeviceSuitability(
            capabilities: ModelCapabilities,
            fileSizeBytes: Long
        ) = DeviceSuitability.Recommended
    }

    private class RecordingRepository : ModelRepository {
        val saved = mutableListOf<ModelMetadata>()
        private val state = MutableStateFlow<List<ModelMetadata>>(emptyList())
        override suspend fun saveModel(model: ModelMetadata) {
            saved.removeAll { it.id == model.id }
            saved += model
            state.value = saved.toList()
        }
        override suspend fun getModelById(modelId: ModelId) = saved.find { it.id == modelId }
        override suspend fun getAllModels() = saved.toList()
        override fun observeAllModels(): Flow<List<ModelMetadata>> = state
        override suspend fun deleteModel(modelId: ModelId) { saved.removeAll { it.id == modelId } }
        override suspend fun deleteAllModels() { saved.clear() }
        override suspend fun modelExists(modelId: ModelId) = saved.any { it.id == modelId }
        override suspend fun updateLastUsed(modelId: ModelId) = Unit
        override suspend fun countModels() = saved.size
    }
}
