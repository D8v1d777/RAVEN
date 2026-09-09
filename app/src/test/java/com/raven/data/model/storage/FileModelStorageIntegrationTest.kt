package com.raven.data.model.storage

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileModelStorageIntegrationTest {
    private lateinit var root: File
    private lateinit var storage: FileModelStorage

    @Before
    fun setUp() {
        root = Files.createTempDirectory("raven-integrated-storage").toFile()
        storage = FileModelStorage(TestPathResolver(root))
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun movesStagedFileIntoManagedModelDirectory() = runBlocking {
        val source = File(root, "source.gguf").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }

        val staged = storage.copyToStaging(source, "operation-1")
        val finalFile = storage.moveFromStagingToFinal(staged, ModelId("model-a"))

        assertEquals(root.resolve("models/.staging/operation-1").canonicalPath, staged.canonicalPath)
        assertEquals(root.resolve("models/model-a/model.gguf").canonicalPath, finalFile.canonicalPath)
        assertTrue(finalFile.exists())
        assertTrue(storage.modelExists(ModelId("model-a")))
        assertTrue(root.resolve("models/.staging").exists())
        assertTrue(root.resolve("models/.staging").listFiles().isNullOrEmpty())
    }

    @Test
    fun rejectsTraversalInStagingName() = runBlocking {
        val source = File(root, "source.gguf").apply { writeText("model") }

        try {
            storage.copyToStaging(source, "../escape")
            throw AssertionError("Expected invalid staging name to fail")
        } catch (error: ModelError.CopyFailed) {
            assertTrue(error.message.orEmpty().contains("Invalid staging"))
        }

        assertFalse(File(root, "escape").exists())
    }

    @Test
    fun calculatesStableSha256ForModelPayload() = runBlocking {
        val source = File(root, "hash-test.gguf").apply {
            writeBytes(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03))
        }

        val hash = storage.calculateSha256(source)

        assertNotNull(hash)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
        assertEquals(64, hash.length)
    }

    @Test
    fun cleanupStagingDirectoryRemovesOldArtifacts() = runBlocking {
        val stageDir = File(root, "models/.staging")
        stageDir.mkdirs()
        val oldFile = File(stageDir, "expired.tmp").apply {
            writeText("old")
            setLastModified(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
        }
        val freshFile = File(stageDir, "fresh.tmp").apply { writeText("new") }

        storage.cleanupStagingDirectory()

        assertFalse(oldFile.exists())
        assertTrue(freshFile.exists())
    }

    private class TestPathResolver(private val root: File) : ModelPathResolver {
        override fun getModelsRootDir(): File = root.resolve("models").also { it.mkdirs() }
        override fun getAvailableStorage(): Long = 1024L * 1024L * 50
        override fun getTotalStorage(): Long = 1024L * 1024L * 50
    }
}