package com.raven.data.model.storage

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileModelStorageTest {
    private lateinit var root: java.io.File
    private lateinit var storage: FileModelStorage

    @Before
    fun setUp() {
        root = Files.createTempDirectory("raven-model-storage").toFile()
        storage = FileModelStorage(TestPathResolver(root))
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun copyCreatesStagingFileAndMoveCreatesManagedModelFile() = runBlocking {
        val source = root.resolve("source.gguf").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        val staged = storage.copyToStaging(source, "operation-1")
        val finalFile = storage.moveFromStagingToFinal(staged, ModelId("model-a"))

        assertEquals(root.resolve("models/.staging/operation-1").canonicalPath, staged.canonicalPath)
        assertEquals(root.resolve("models/model-a/model.gguf").canonicalPath, finalFile.canonicalPath)
        assertTrue(finalFile.readBytes().contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(storage.modelExists(ModelId("model-a")))
        assertTrue(storage.listModels().single().name == "model-a")
    }

    @Test
    fun deletingOneModelDoesNotDeleteAnother() = runBlocking {
        val source = root.resolve("source.gguf").apply { writeText("model") }
        storage.moveFromStagingToFinal(storage.copyToStaging(source, "one"), ModelId("one"))
        storage.moveFromStagingToFinal(storage.copyToStaging(source, "two"), ModelId("two"))

        storage.deleteModel(ModelId("one"))

        assertFalse(storage.modelExists(ModelId("one")))
        assertTrue(storage.modelExists(ModelId("two")))
    }

    @Test
    fun traversalInStagingNameIsRejected() = runBlocking {
        val source = root.resolve("source.gguf").apply { writeText("model") }

        try {
            storage.copyToStaging(source, "../escape")
            org.junit.Assert.fail("Expected invalid staging name")
        } catch (error: ModelError.CopyFailed) {
            assertTrue(error.message!!.contains("Invalid staging"))
        }
        assertFalse(root.resolve("escape").exists())
    }

    @Test
    fun missingModelIsReportedAsAbsent() = runBlocking {
        assertFalse(storage.modelExists(ModelId("missing")))
        assertEquals(null, storage.getModelFile(ModelId("missing")))
    }

    private class TestPathResolver(private val root: java.io.File) : ModelPathResolver {
        override fun getModelsRootDir() = root.resolve("models")
        override fun getAvailableStorage() = 1024L * 1024L
        override fun getTotalStorage() = 1024L * 1024L
    }
}
