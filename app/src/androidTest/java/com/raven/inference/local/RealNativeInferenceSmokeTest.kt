package com.raven.inference.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.raven.data.db.RavenDatabase
import com.raven.data.model.repository.RoomModelRepository
import com.raven.data.model.storage.AndroidModelPathResolver
import com.raven.data.model.storage.FileModelStorage
import com.raven.data.model.validation.BasicGgufValidator
import com.raven.domain.ChatMessage
import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.Role
import com.raven.domain.model.usecase.ImportModelUseCase
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealNativeInferenceSmokeTest {
    private lateinit var database: RavenDatabase
    private lateinit var runtime: LlamaCppInferenceRuntime
    private lateinit var provider: LocalLlmProvider

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, RavenDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val repository = RoomModelRepository(database.modelDao())
        val pathResolver = AndroidModelPathResolver(context)
        runtime = LlamaCppInferenceRuntime(context, pathResolver)
        provider = LocalLlmProvider(repository, runtime)
    }

    @After
    fun tearDown() = runBlocking {
        provider.unload().getOrThrow()
        database.close()
    }

    @Test
    fun externalGgufLoadsGeneratesUnloadsAndReloads() = runBlocking {
        val externalPath = InstrumentationRegistry.getArguments().getString(ARG_GGUF_PATH)
            ?: error("Provide -e $ARG_GGUF_PATH /device/path/model.gguf")
        val sourceFile = File(externalPath)
        require(sourceFile.isFile && sourceFile.canRead()) {
            "External GGUF is not readable: $externalPath"
        }

        val context: Context = ApplicationProvider.getApplicationContext()
        val pathResolver = AndroidModelPathResolver(context)
        val storage = FileModelStorage(pathResolver)
        val validator = BasicGgufValidator(context, storage::calculateSha256)
        val repository = RoomModelRepository(database.modelDao())
        val metadata = ImportModelUseCase(storage, validator, repository)
            .importModel(sourceFile)

        assertTrue(pathResolver.getModelFile(metadata.id).isFile)
        assertEquals(metadata, repository.getModelById(metadata.id))

        val request = GenerationRequest(
            modelId = metadata.id.value,
            systemPrompt = "Reply with exactly: RAVEN_NATIVE_OK",
            messages = listOf(
                ChatMessage(
                    id = "native-smoke-user",
                    role = Role.USER,
                    content = "Reply with exactly: RAVEN_NATIVE_OK",
                    createdAtEpochMs = 0L,
                )
            ),
            temperature = 0.0f,
            topP = 1.0f,
            maxOutputTokens = 16,
        )

        assertGeneration(provider, request)
        provider.unload().getOrThrow()
        assertTrue(!runtime.isModelLoaded())
        provider.selectModel(metadata.id.value).getOrThrow()
        assertGeneration(provider, request)
    }

    private suspend fun assertGeneration(
        provider: LocalLlmProvider,
        request: GenerationRequest,
    ) {
        val events = provider.stream(request).toList()
        assertNotNull(events.firstOrNull { it === GenerationEvent.Started })
        val tokens = events.filterIsInstance<GenerationEvent.Token>()
        val completed = events.filterIsInstance<GenerationEvent.Completed>()
        assertTrue("llama.cpp returned no token events: $events", tokens.isNotEmpty())
        assertEquals("Generation did not complete normally: $events", 1, completed.size)
        assertTrue(tokens.joinToString(separator = "") { it.text }.isNotBlank())
    }

    companion object {
        const val ARG_GGUF_PATH = "raven.gguf.path"
    }
}
