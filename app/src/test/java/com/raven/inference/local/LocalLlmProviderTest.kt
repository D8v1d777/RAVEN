package com.raven.inference.local

import com.raven.domain.ChatMessage
import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.Role
import com.raven.domain.model.ModelCapabilities
import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.ModelSource
import com.raven.domain.model.ModelStatus
import com.raven.domain.model.repository.ModelRepository
import com.raven.inference.provider.ProviderHealth
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real behaviour tests for [LocalLlmProvider].
 *
 * These assert on actual state changes and emitted events instead of asserting that a
 * placeholder value equals itself.
 */
class LocalLlmProviderTest {

    private val gemma = metadata("gemma-2b-it-q4_k_m-abc123abc123", "gemma-2b-it-q4_k_m")
    private val qwen = metadata("qwen2-0_5b-instruct-def456def456", "qwen2-0_5b-instruct")

    private fun metadata(id: String, name: String) = ModelMetadata(
        id = ModelId(id),
        displayName = name,
        fileName = "$name.gguf",
        fileSizeBytes = 1_500_000_000L,
        status = ModelStatus.Installed,
        source = ModelSource.IMPORTED,
        capabilities = ModelCapabilities(quantization = "Q4_K_M"),
    )

    private fun request(modelId: String, maxTokens: Int = 8) = GenerationRequest(
        modelId = modelId,
        systemPrompt = "test system prompt",
        messages = listOf(
            ChatMessage("u1", Role.USER, "hello", 0L),
        ),
        temperature = 0.1f,
        topP = 1.0f,
        maxOutputTokens = maxTokens,
    )

    @Test
    fun `provider identity describes an on-device runtime`() {
        val provider = LocalLlmProvider(FakeRepository(), FakeRuntime())

        assertEquals("local", provider.id)
        assertTrue(provider.runsOnDevice)
        assertNull(provider.selectedModel.value)
    }

    @Test
    fun `listModels maps every registry entry to a descriptor`() = runTest {
        val provider = LocalLlmProvider(FakeRepository(gemma, qwen), FakeRuntime())

        val descriptors = provider.listModels().getOrThrow()

        assertEquals(2, descriptors.size)
        assertEquals(
            listOf("gemma-2b-it-q4_k_m", "qwen2-0_5b-instruct"),
            descriptors.map { it.displayName },
        )
        assertTrue(descriptors.all { it.providerId == "local" })
        assertTrue(descriptors.all { it.format == "GGUF" })
        assertTrue(descriptors.all { it.quantization == "Q4_K_M" })
    }

    @Test
    fun `health reports unavailable without models and degraded before a load`() = runTest {
        val empty = LocalLlmProvider(FakeRepository(), FakeRuntime())
        assertTrue(empty.health() is ProviderHealth.Unavailable)

        val provider = LocalLlmProvider(FakeRepository(gemma), FakeRuntime())
        assertTrue(provider.health() is ProviderHealth.Degraded)
    }

    @Test
    fun `health reports ready once a model is loaded`() = runTest {
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        provider.selectModel(gemma.id.value).getOrThrow()

        assertTrue(provider.health() is ProviderHealth.Ready)
        assertEquals(listOf(gemma.displayName), runtime.loadedModels)
    }

    @Test
    fun `selectModel loads through the runtime and records last use`() = runTest {
        val repository = FakeRepository(gemma, qwen)
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(repository, runtime)

        provider.selectModel(gemma.id.value).getOrThrow()

        assertTrue(runtime.isModelLoaded())
        assertEquals(gemma.id.value, provider.selectedModel.value?.id)
        assertEquals(listOf(gemma.id.value), repository.lastUsedUpdates)
    }

    @Test
    fun `selectModel on an already loaded model does not reload the runtime`() = runTest {
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        provider.selectModel(gemma.id.value).getOrThrow()
        provider.selectModel(gemma.id.value).getOrThrow()

        assertEquals(1, runtime.loadedModels.size)
    }

    @Test
    fun `selectModel fails with NotFound for an unknown model and never touches the runtime`() = runTest {
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        val result = provider.selectModel("missing-model")

        assertTrue(result.exceptionOrNull() is ModelError.NotFound)
        assertTrue(runtime.loadedModels.isEmpty())
    }

    @Test
    fun `selectModel rejects an invalid identifier`() = runTest {
        val provider = LocalLlmProvider(FakeRepository(gemma), FakeRuntime())

        val result = provider.selectModel("not a valid id!")

        assertTrue(result.exceptionOrNull() is ModelError.UnsupportedModel)
    }

    @Test
    fun `a failed switch restores the previously loaded model`() = runTest {
        val repository = FakeRepository(gemma, qwen)
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(repository, runtime)

        provider.selectModel(gemma.id.value).getOrThrow()

        runtime.failLoadWith = ModelError.ModelLoadFailed("simulated llama.cpp failure")
        val result = provider.selectModel(qwen.id.value)

        assertTrue(result.isFailure)
        // The failed qwen load was attempted, and rollback reloaded gemma. A failed
        // load never registers as a *successful* load in the runtime, so the
        // successful-load history shows gemma twice.
        assertEquals(
            listOf(gemma.displayName, qwen.displayName, gemma.displayName),
            runtime.loadAttempts,
        )
        assertEquals(
            listOf(gemma.displayName, gemma.displayName),
            runtime.loadedModels,
        )
        assertEquals("rollback unloads the dead slot first", 1, runtime.unloadCount)
        assertEquals(gemma.id.value, provider.selectedModel.value?.id)
    }

    @Test
    fun `unload clears the selection`() = runTest {
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)
        provider.selectModel(gemma.id.value).getOrThrow()

        provider.unload().getOrThrow()

        assertNull(provider.selectedModel.value)
        assertFalse(runtime.isModelLoaded())
    }

    @Test
    fun `resetSession is forwarded to the runtime`() = runTest {
        val runtime = FakeRuntime()
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)
        provider.selectModel(gemma.id.value).getOrThrow()

        provider.resetSession().getOrThrow()

        assertEquals(1, runtime.resetCount)
    }

    @Test
    fun `stream emits started tokens and a completed event with the real token count`() = runTest {
        val runtime = FakeRuntime(tokens = listOf("Hello", " there", "!"))
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        val events = provider.stream(request(gemma.id.value)).toList()

        assertTrue(events.first() is GenerationEvent.Started)
        val tokens = events.filterIsInstance<GenerationEvent.Token>().map { it.text }
        assertEquals(listOf("Hello", " there", "!"), tokens)
        val completed = events.filterIsInstance<GenerationEvent.Completed>().single()
        assertEquals(3, completed.outputTokens)
        assertNull("prompt token count is unknown for the local runtime", completed.inputTokens)
    }

    @Test
    fun `stream forwards the request to the runtime`() = runTest {
        val runtime = FakeRuntime(tokens = listOf("ok"))
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        provider.stream(request(gemma.id.value, maxTokens = 4)).toList()

        val forwarded = runtime.beginGenerationRequest
        assertEquals(gemma.id.value, forwarded?.modelId)
        assertEquals(4, forwarded?.maxOutputTokens)
        assertEquals("test system prompt", forwarded?.systemPrompt)
    }

    @Test
    fun `stream stops at maxOutputTokens`() = runTest {
        val runtime = FakeRuntime(tokens = List(50) { "t" })
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        val events = provider.stream(request(gemma.id.value, maxTokens = 5)).toList()

        assertEquals(5, events.filterIsInstance<GenerationEvent.Token>().size)
        assertEquals(5, events.filterIsInstance<GenerationEvent.Completed>().single().outputTokens)
    }

    @Test
    fun `stream skips empty token strings without counting them`() = runTest {
        val runtime = FakeRuntime(tokens = listOf("a", "", "b"))
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        val events = provider.stream(request(gemma.id.value)).toList()

        assertEquals(2, events.filterIsInstance<GenerationEvent.Token>().size)
        assertEquals(2, events.filterIsInstance<GenerationEvent.Completed>().single().outputTokens)
    }

    @Test
    fun `stream fails with a stable code when the model cannot be loaded`() = runTest {
        val runtime = FakeRuntime()
        runtime.failLoadWith = ModelError.ModelLoadFailed("no memory")
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        val events = provider.stream(request(gemma.id.value)).toList()

        val failure = events.filterIsInstance<GenerationEvent.Failed>().single()
        assertEquals("MODEL_LOAD_FAILED", failure.code)
        assertTrue(events.none { it is GenerationEvent.Started })
    }

    @Test
    fun `cancelling the stream signals the native runtime`() = runTest {
        val runtime = FakeRuntime(tokens = listOf("partial"), hangWhenDrained = true)
        val provider = LocalLlmProvider(FakeRepository(gemma), runtime)

        // Load up-front so the background stream never hops to Dispatchers.Default;
        // that hop re-enqueues asynchronously and races with advanceUntilIdle().
        provider.selectModel(gemma.id.value).getOrThrow()

        val job = backgroundScope.launch {
            provider.stream(
                GenerationRequest(
                    modelId = gemma.id.value,
                    systemPrompt = "p",
                    messages = emptyList(),
                    temperature = 0.1f,
                    topP = 1.0f,
                    maxOutputTokens = 8,
                )
            ).toList()
        }
        runCurrent()
        advanceUntilIdle()
        assertTrue(runtime.beginGenerationRequest != null)

        job.cancelAndJoin()
        advanceUntilIdle()

        assertEquals("the Stop button must reach native cancellation", 1, runtime.cancelCount)
    }

    @Test
    fun `stream reports MODEL_NOT_FOUND for an unknown model`() = runTest {
        val provider = LocalLlmProvider(FakeRepository(gemma), FakeRuntime())

        val events = provider.stream(request("ghost-model")).toList()

        assertEquals(
            "MODEL_NOT_FOUND",
            events.filterIsInstance<GenerationEvent.Failed>().single().code,
        )
    }
}

/** In-memory registry. No Android, no Room, no filesystem. */
internal class FakeRepository(
    vararg initial: ModelMetadata,
) : ModelRepository {

    private val models = linkedMapOf<String, ModelMetadata>().apply {
        initial.forEach { put(it.id.value, it) }
    }

    val lastUsedUpdates = mutableListOf<String>()

    override suspend fun saveModel(model: ModelMetadata) {
        models[model.id.value] = model
    }

    override suspend fun getModelById(modelId: ModelId): ModelMetadata? = models[modelId.value]

    override suspend fun getAllModels(): List<ModelMetadata> = models.values.toList()

    override fun observeAllModels(): Flow<List<ModelMetadata>> = flow { emit(models.values.toList()) }

    override suspend fun deleteModel(modelId: ModelId) {
        models.remove(modelId.value)
    }

    override suspend fun deleteAllModels() = models.clear()

    override suspend fun modelExists(modelId: ModelId): Boolean = models.containsKey(modelId.value)

    override suspend fun updateLastUsed(modelId: ModelId) {
        lastUsedUpdates.add(modelId.value)
    }

    override suspend fun countModels(): Int = models.size
}

/** Deterministic stand-in for the native runtime. Records every call it receives. */
internal class FakeRuntime(
    private val tokens: List<String> = emptyList(),
    private val hangWhenDrained: Boolean = false,
) : LocalInferenceRuntime {

    private val pending = ArrayDeque<String>()
    private var current: ModelMetadata? = null

    /** One-shot load failure: the next [loadModel] throws once, then the injection clears. */
    var failLoadWith: Throwable? = null
    var beginGenerationRequest: GenerationRequest? = null
    var cancelCount = 0
    var resetCount = 0
    var unloadCount = 0
    val loadAttempts = mutableListOf<String>()
    val loadedModels = mutableListOf<String>()

    override suspend fun loadModel(metadata: ModelMetadata) {
        loadAttempts.add(metadata.displayName)
        failLoadWith?.let { failure ->
            // One-shot: each load fails (or not) on its own merits, so a rollback load
            // of a different model is not doomed by an earlier injected failure.
            failLoadWith = null
            throw failure
        }
        current = metadata
        pending.clear()
        pending.addAll(tokens)
        loadedModels.add(metadata.displayName)
    }

    override suspend fun unloadModel() {
        current = null
        unloadCount++
    }

    override suspend fun resetSession() {
        resetCount++
        pending.clear()
        pending.addAll(tokens)
    }

    override suspend fun cancelGeneration() {
        cancelCount++
        pending.clear()
    }

    override fun isModelLoaded(): Boolean = current != null

    override fun getCurrentModel(): ModelMetadata? = current

    override suspend fun beginGeneration(request: GenerationRequest) {
        beginGenerationRequest = request
    }

    override suspend fun generateToken(): String? {
        if (pending.isEmpty() && hangWhenDrained) awaitCancellation()
        return if (pending.isEmpty()) null else pending.removeFirst()
    }
}