package com.raven.chat

import com.raven.domain.GenerationEvent
import com.raven.domain.GenerationRequest
import com.raven.domain.ModelDescriptor
import com.raven.domain.Role
import com.raven.inference.provider.LlmProvider
import com.raven.inference.provider.ProviderHealth
import com.raven.inference.router.ProviderRouter
import com.raven.persona.RavenPersona
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
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
 * End-to-end tests for the M1 chat loop, without Android or llama.cpp.
 *
 * They drive the same path the app uses:
 *   ConversationEngine -> ProviderRouter -> LlmProvider -> generation events -> UI state
 */
class ConversationEngineTest {

    private fun routerFor(provider: LlmProvider) = ProviderRouter().apply {
        registerProvider(provider)
    }

    private fun completed(vararg tokens: String): suspend (GenerationRequest) -> Flow<GenerationEvent> =
        { flow { emit(GenerationEvent.Started); tokens.forEach { emit(GenerationEvent.Token(it)) }; emit(GenerationEvent.Completed(outputTokens = tokens.size)) } }

    private fun failing(code: String, message: String): suspend (GenerationRequest) -> Flow<GenerationEvent> =
        { flow { emit(GenerationEvent.Started); emit(GenerationEvent.Failed(code, message)) } }

    private fun hanging(firstToken: String): suspend (GenerationRequest) -> Flow<GenerationEvent> =
        { flow { emit(GenerationEvent.Started); emit(GenerationEvent.Token(firstToken)); awaitCancellation() } }

    @Test
    fun `blank input never enters the conversation`() = runTest {
        val provider = FakeProvider(completed("hi")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("   ")
        engine.send("")

        assertTrue(engine.state.value.messages.isEmpty())
        assertNull(provider.lastRequest)
    }

    @Test
    fun `a completed turn renders the user message then the streamed reply`() = runTest {
        val provider = FakeProvider(completed("Hi", " there", "!")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()

        val messages = engine.state.value.messages
        assertEquals(2, messages.size)
        assertEquals(Role.USER, messages[0].role)
        assertEquals("hello", messages[0].text)
        assertEquals(Role.ASSISTANT, messages[1].role)
        assertEquals("Hi there!", messages[1].text)
        assertEquals(MessageStatus.COMPLETE, messages[1].status)
        assertEquals(3, messages[1].outputTokenCount)
        assertEquals(ChatPhase.IDLE, engine.state.value.phase)
        assertEquals("fake-model", engine.state.value.modelId)
        assertNull(engine.state.value.lastError)
    }

    @Test
    fun `the request carries the persona prompt and the full history`() = runTest {
        val provider = FakeProvider(completed("one")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("first")
        runCurrent(); runCurrent(); advanceUntilIdle()
        engine.send("second")
        runCurrent(); runCurrent(); advanceUntilIdle()

        val secondRequest = provider.requests[1]
        assertEquals(RavenPersona.systemPrompt(), secondRequest.systemPrompt)
        assertEquals(
            listOf("first", "one", "second"),
            secondRequest.messages.map { it.content },
        )
    }

    @Test
    fun `generation without a loaded model is reported as MODEL_NOT_LOADED and keeps the user message`() = runTest {
        val provider = FakeProvider(completed("never"))
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()

        val messages = engine.state.value.messages
        assertEquals(2, messages.size)
        assertEquals("hello", messages[0].text)
        assertEquals(MessageStatus.FAILED, messages[1].status)
        val error = engine.state.value.lastError
        assertEquals("MODEL_NOT_LOADED", error?.code)
        assertFalse(error?.retryable == true)
        assertNull(provider.lastRequest)
        assertEquals(ChatPhase.IDLE, engine.state.value.phase)
    }

    @Test
    fun `a provider failure keeps the stable code and offers retry`() = runTest {
        val provider = FakeProvider(failing("OUT_OF_MEMORY_RISK", "not enough memory")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()

        val error = engine.state.value.lastError
        assertEquals("OUT_OF_MEMORY_RISK", error?.code)
        assertEquals("not enough memory", error?.message)
        assertTrue(error?.retryable == true)
        assertTrue(engine.state.value.canRetry)
        assertEquals(MessageStatus.FAILED, engine.state.value.messages.last().status)
    }

    @Test
    fun `stop cancels the active generation and preserves the partial reply`() = runTest {
        val provider = FakeProvider(hanging("Partial answer")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()
        assertEquals(ChatPhase.GENERATING, engine.state.value.phase)

        engine.stop()
        runCurrent(); runCurrent(); advanceUntilIdle()

        val reply = engine.state.value.messages.last()
        assertEquals("Partial answer", reply.text)
        assertEquals(MessageStatus.CANCELLED, reply.status)
        assertEquals(ChatPhase.IDLE, engine.state.value.phase)
    }

    @Test
    fun `a cancelled turn is not recorded as model history`() = runTest {
        val provider = FakeProvider(hanging("Partial")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("first")
        runCurrent(); runCurrent(); advanceUntilIdle()
        engine.stop()
        runCurrent(); runCurrent(); advanceUntilIdle()

        provider.script = completed("second answer")
        engine.send("second")
        runCurrent(); runCurrent(); advanceUntilIdle()

        // The partial reply never reached the model, so it must not be replayed to it.
        assertEquals(
            listOf("first", "second"),
            provider.requests.last().messages.map { it.content },
        )
    }

    @Test
    fun `only one generation runs at a time`() = runTest {
        val provider = FakeProvider(hanging("Partial")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("first")
        runCurrent(); runCurrent(); advanceUntilIdle()
        engine.send("second")
        runCurrent(); runCurrent(); advanceUntilIdle()

        assertEquals(1, provider.requests.size)
        assertEquals(2, engine.state.value.messages.size)
    }

    @Test
    fun `retry drops the failed reply and re-runs the last user turn`() = runTest {
        val provider = FakeProvider(failing("GENERATION_FAILED", "boom")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()
        assertEquals(2, engine.state.value.messages.size)

        provider.script = completed("recovered")
        engine.retry()
        runCurrent(); runCurrent(); advanceUntilIdle()

        val messages = engine.state.value.messages
        assertEquals(2, messages.size)
        assertEquals("hello", messages[0].text)
        assertEquals("recovered", messages[1].text)
        assertEquals(MessageStatus.COMPLETE, messages[1].status)
        assertNull(engine.state.value.lastError)
    }

    @Test
    fun `retry does nothing when no user message exists`() = runTest {
        val provider = FakeProvider(completed("x")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.retry()
        runCurrent(); runCurrent(); advanceUntilIdle()

        assertTrue(provider.requests.isEmpty())
        assertTrue(engine.state.value.messages.isEmpty())
    }

    @Test
    fun `starting a new conversation clears the transcript and resets the provider session`() = runTest {
        val provider = FakeProvider(completed("hi")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()
        engine.clearConversation()
        runCurrent(); runCurrent(); advanceUntilIdle()

        assertTrue(engine.state.value.messages.isEmpty())
        assertEquals(1, provider.resetSessionCount)
    }

    @Test
    fun `dismissing an error keeps the transcript`() = runTest {
        val provider = FakeProvider(failing("GENERATION_FAILED", "boom")).apply { load() }
        val engine = ConversationEngine(routerFor(provider), backgroundScope)

        engine.send("hello")
        runCurrent(); runCurrent(); advanceUntilIdle()
        engine.dismissError()

        assertNull(engine.state.value.lastError)
        assertEquals(2, engine.state.value.messages.size)
    }

    /** Configurable provider double. Streams a scripted flow and records every request. */
internal class FakeProvider(
    var script: suspend (GenerationRequest) -> Flow<GenerationEvent>,
) : LlmProvider {

    override val id: String = "fake"
    override val displayName: String = "Fake provider"
    override val runsOnDevice: Boolean = true

    private val _selected = MutableStateFlow<ModelDescriptor?>(null)
    override val selectedModel: StateFlow<ModelDescriptor?> = _selected.asStateFlow()

    val requests = mutableListOf<GenerationRequest>()
    val lastRequest: GenerationRequest? get() = requests.lastOrNull()
    var resetSessionCount = 0

    fun load(id: String = "fake-model", name: String = "Fake Model") {
        _selected.value = ModelDescriptor(id = id, displayName = name, providerId = this.id)
    }

    override suspend fun listModels(): Result<List<ModelDescriptor>> =
        Result.success(_selected.value?.let { listOf(it) } ?: emptyList())

    override fun stream(request: GenerationRequest): Flow<GenerationEvent> = flow {
        requests.add(request)
        emitAll(script(request))
    }

    override suspend fun health(): ProviderHealth = ProviderHealth.Ready

    override suspend fun selectModel(modelId: String): Result<Unit> {
        load(modelId)
        return Result.success(Unit)
    }

    override suspend fun getSelectedModel(): ModelDescriptor? = _selected.value

    override suspend fun resetSession(): Result<Unit> {
        resetSessionCount++
        return Result.success(Unit)
    }

    override suspend fun unload(): Result<Unit> {
        _selected.value = null
        return Result.success(Unit)
    }
}
}