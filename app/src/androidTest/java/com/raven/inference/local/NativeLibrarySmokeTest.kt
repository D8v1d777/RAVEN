package com.raven.inference.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raven.inference.local.LlamaCppInferenceRuntime
import com.raven.data.model.storage.ModelPathResolver
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Smoke test to verify the native Raven llama library is properly built, packaged, and loadable.
 * This test checks:
 * 1. System.loadLibrary("raven_llama") succeeds
 * 2. A native method (cancelGeneration) can be invoked safely without a model
 */
@RunWith(AndroidJUnit4::class)
class NativeLibrarySmokeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `raven llama native library loads and native function executes successfully`() = runBlocking {
        // Arrange: Create a minimal path resolver that uses cache directory
        val pathResolver = object : ModelPathResolver {
            override fun getModelsRootDir(): File = File(context.cacheDir, "test_models")
            override fun getAvailableStorage(): Long = 1024L * 1024L * 100 // 100 MB
            override fun getTotalStorage(): Long = 1024L * 1024L * 100 // 100 MB
        }

        // Act & Assert: Verify the native library loads and a native function works
        // This test will fail fast if:
        // 1. Library not packaged in APK (UnsatisfiedLinkError on loadLibrary)
        // 2. JNI symbol not found (UnsatisfiedLinkError on nativeCancelGeneration call)
        // 3. Native initialization fails (exception from native code)
        try {
            // This constructor triggers the companion object init which loads the library
            val runtime = LlamaCppInferenceRuntime(context, pathResolver)
            // This invokes the nativeCancelGeneration JNI function directly
            // which is safe to call without a model loaded
            runtime.cancelGeneration()
            assertTrue(true)
        } catch (e: Exception) {
            assertTrue(false)
        }
    }
}