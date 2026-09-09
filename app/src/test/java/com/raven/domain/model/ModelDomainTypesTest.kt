package com.raven.domain.model

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

class ModelIdTest {
    
    @Test
    fun validModelIdCreated() {
        val id = ModelId("test-model-1")
        assertEquals("test-model-1", id.value)
    }
    
    @Test
    fun invalidModelIdThrows() {
        assertInvalidId("")
        assertInvalidId("   ")
        assertInvalidId("model with spaces")
        assertInvalidId("model@special")
    }
    
    @Test
    fun modelIdToString() {
        val id = ModelId("test")
        assertEquals("test", id.toString())
    }

    private fun assertInvalidId(value: String) {
        try {
            ModelId(value)
            fail("Expected invalid model ID")
        } catch (_: IllegalArgumentException) {
        }
    }
}

class ModelStatusTest {
    
    @Test
    fun statusUserMessage() {
        assertEquals("Ready", ModelStatus.Ready.userMessage())
        assertEquals("Loaded", ModelStatus.Loaded.userMessage())
        assertTrue(ModelStatus.Loaded.userMessage().contains("Loaded"))
    }
    
    @Test
    fun isReady() {
        assertTrue(ModelStatus.Ready.isReady())
        assertFalse(ModelStatus.Loaded.isReady())
        assertFalse(ModelStatus.Discovered.isReady())
    }
    
    @Test
    fun isLoaded() {
        assertTrue(ModelStatus.Loaded.isLoaded())
        assertFalse(ModelStatus.Ready.isLoaded())
    }
    
    @Test
    fun isError() {
        assertTrue(ModelStatus.LoadFailed("test").isError())
        assertTrue(ModelStatus.ChecksumFailed("exp", "act").isError())
        assertFalse(ModelStatus.Ready.isError())
        assertFalse(ModelStatus.Loaded.isError())
    }
}

class ModelCapabilitiesTest {
    
    @Test
    fun emptyCapabilitiesHasNoMetadata() {
        val empty = ModelCapabilities()
        assertFalse(empty.hasMetadata())
    }
    
    @Test
    fun capabilitiesWithArchitecture() {
        val caps = ModelCapabilities(architecture = "llama")
        assertTrue(caps.hasMetadata())
    }
    
    @Test
    fun displayCapabilities() {
        val caps = ModelCapabilities(
            architecture = "llama",
            quantization = "Q4_K_M",
            parameterCount = 7,
            contextLength = 2048
        )
        val display = caps.displayCapabilities()
        assertTrue(display.contains("llama"))
        assertTrue(display.contains("Q4_K_M"))
        assertTrue(display.contains("7M"))
        assertTrue(display.contains("2048"))
    }
}

class ModelMetadataTest {
    
    @Test
    fun validMetadata() {
        val metadata = ModelMetadata(
            id = ModelId("test"),
            displayName = "Test Model",
            fileName = "test.gguf",
            fileSizeBytes = 1024L
        )
        assertEquals("Test Model", metadata.displayName)
        assertFalse(metadata.isValid()) // Status is Discovered, not Ready
    }
    
    @Test
    fun readyMetadataIsValid() {
        val metadata = ModelMetadata(
            id = ModelId("test"),
            displayName = "Test Model",
            fileName = "test.gguf",
            fileSizeBytes = 1024L,
            status = ModelStatus.Ready
        )
        assertTrue(metadata.isValid())
    }
    
    @Test
    fun invalidMetadataThrows() {
        assertInvalidMetadata {
            ModelMetadata(
                id = ModelId("test"),
                displayName = "",
                fileName = "test.gguf",
                fileSizeBytes = 1024L
            )
        }
        assertInvalidMetadata {
            ModelMetadata(
                id = ModelId("test"),
                displayName = "Test",
                fileName = "",
                fileSizeBytes = 1024L
            )
        }
        assertInvalidMetadata {
            ModelMetadata(
                id = ModelId("test"),
                displayName = "Test",
                fileName = "test.gguf",
                fileSizeBytes = 0L
            )
        }
    }

    private fun assertInvalidMetadata(create: () -> ModelMetadata) {
        try {
            create()
            fail("Expected invalid metadata")
        } catch (_: IllegalArgumentException) {
        }
    }

}
