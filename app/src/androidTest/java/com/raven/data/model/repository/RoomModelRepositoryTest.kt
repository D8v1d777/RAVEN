package com.raven.data.model.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raven.data.db.RavenDatabase
import com.raven.domain.model.ModelCapabilities
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.ModelSource
import com.raven.domain.model.ModelStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomModelRepositoryTest {
    private lateinit var modelRepository: RoomModelRepository
    private lateinit var ravenDatabase: RavenDatabase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        ravenDatabase = Room.inMemoryDatabaseBuilder(
            context,
            RavenDatabase::class.java
        ).allowMainThreadQueries().build()
        modelRepository = RoomModelRepository(ravenDatabase.modelDao())
    }

    @After
    fun tearDown() {
        ravenDatabase.close()
    }

    @Test
    fun saveAndRetrieveModel() = runBlocking {
        val model = ModelMetadata(
            id = ModelId("test-model"),
            displayName = "Test Model",
            fileName = "test-model.gguf",
            fileSizeBytes = 1024L,
            source = ModelSource.IMPORTED,
            status = ModelStatus.Ready,
            capabilities = ModelCapabilities(
                architecture = "llama",
                quantization = "Q4_K_M",
                parameterCount = 7,
                contextLength = 2048
            )
        )

        modelRepository.saveModel(model)
        val restored = modelRepository.getModelById(model.id)

        assertNotNull(restored)
        assertEquals(model, restored)
    }

    @Test
    fun multipleModelsAndDeletionArePersisted() = runBlocking {
        val first = model("model-1", ModelSource.IMPORTED)
        val second = model("model-2", ModelSource.DOWNLOADED)

        modelRepository.saveModel(first)
        modelRepository.saveModel(second)
        assertEquals(2, modelRepository.countModels())
        assertTrue(modelRepository.modelExists(first.id))
        assertTrue(modelRepository.modelExists(second.id))

        modelRepository.deleteModel(first.id)

        assertFalse(modelRepository.modelExists(first.id))
        assertTrue(modelRepository.modelExists(second.id))
    }

    @Test
    fun missingModelReturnsNull() = runBlocking {
        assertNull(modelRepository.getModelById(ModelId("missing")))
    }

    private fun model(id: String, source: ModelSource) = ModelMetadata(
        id = ModelId(id),
        displayName = id,
        fileName = "$id.gguf",
        fileSizeBytes = 1024L,
        source = source,
        status = ModelStatus.Installed
    )
}
