package com.raven.data.model.db

import com.raven.domain.model.ModelCapabilities
import com.raven.domain.model.ModelId
import com.raven.domain.model.ModelMetadata
import com.raven.domain.model.ModelSource
import com.raven.domain.model.ModelStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelEntityTest {
    @Test
    fun installedStatusRoundTrips() {
        val original = metadata(ModelStatus.Installed)

        val restored = ModelEntity.fromDomain(original).toDomain()

        assertEquals(original.id, restored.id)
        assertEquals(original.status, restored.status)
        assertEquals(original.capabilities, restored.capabilities)
    }

    @Test
    fun loadingStatusRoundTrips() {
        val original = metadata(ModelStatus.Loading)

        val restored = ModelEntity.fromDomain(original).toDomain()

        assertEquals(ModelStatus.Loading, restored.status)
    }

    @Test
    fun failureStatusRoundTripsReason() {
        val original = metadata(ModelStatus.InvalidFormat("bad header: GGUF missing"))

        val restored = ModelEntity.fromDomain(original).toDomain()

        assertEquals(original.status, restored.status)
    }

    private fun metadata(status: ModelStatus) = ModelMetadata(
        id = ModelId("test-model"),
        displayName = "Test Model",
        fileName = "test.gguf",
        fileSizeBytes = 2048L,
        status = status,
        source = ModelSource.IMPORTED,
        capabilities = ModelCapabilities(architecture = "llama", quantization = "Q4_K_M")
    )
}
