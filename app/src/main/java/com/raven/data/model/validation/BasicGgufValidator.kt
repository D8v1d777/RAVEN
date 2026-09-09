package com.raven.data.model.validation

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.raven.domain.model.ModelCapabilities
import com.raven.domain.model.ModelError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Implementation of GGUF validation.
 * Performs multi-layer validation and metadata extraction.
 * Note: Full metadata extraction requires llama.cpp integration (STEP 10).
 */
class BasicGgufValidator(
    private val context: Context,
    private val sha256Calculator: suspend (File) -> String
) : GgufValidator {
    
    companion object {
        // GGUF magic bytes: "GGUF" = 0x47 0x47 0x55 0x46
        private val GGUF_MAGIC = byteArrayOf(0x47, 0x47, 0x55, 0x46)
        
        // Minimum GGUF file size (header + minimal model)
        private const val MIN_GGUF_SIZE = 1024L
        
        // Heuristic RAM requirement multipliers (conservative estimates)
        // Assumes model uses roughly: file_size + context_length * 128 bytes
        private const val RAM_MULTIPLIER = 2.0f
    }
    
    override suspend fun validateFileExists(file: File) {
        if (!file.exists()) {
            throw ModelError.InvalidFile("File does not exist: ${file.absolutePath}")
        }
        if (!file.isFile) {
            throw ModelError.InvalidFile("Path is not a file: ${file.absolutePath}")
        }
        if (file.length() == 0L) {
            throw ModelError.InvalidFile("File is empty: ${file.absolutePath}")
        }
        if (file.length() < MIN_GGUF_SIZE) {
            throw ModelError.InvalidFile("File too small to be a valid GGUF: ${file.length()} bytes")
        }
    }
    
    override suspend fun validateChecksum(
        file: File,
        expectedSha256: String
    ) = withContext(Dispatchers.IO) {
        val actualSha256 = sha256Calculator(file)
        if (actualSha256.lowercase() != expectedSha256.lowercase()) {
            throw ModelError.ChecksumMismatch(expectedSha256, actualSha256)
        }
    }
    
    override suspend fun validateGgufFormat(file: File) = withContext(Dispatchers.IO) {
        file.inputStream().use { input ->
            val magicBytes = ByteArray(4)
            val bytesRead = input.read(magicBytes)
            if (bytesRead < 4) {
                throw ModelError.InvalidFile("File too small to contain GGUF header")
            }
            if (!magicBytes.contentEquals(GGUF_MAGIC)) {
                throw ModelError.InvalidFile("File does not have valid GGUF magic bytes")
            }
        }
    }
    
    override suspend fun extractMetadata(file: File): ModelCapabilities = withContext(Dispatchers.IO) {
        try {
            // Basic metadata extraction (stub)
            // A complete implementation would:
            // 1. Parse GGUF header and kv-pairs
            // 2. Extract model architecture, quantization, context length, etc.
            // 3. This requires either:
            //    a. llama.cpp JNI calls (STEP 10)
            //    b. Pure Kotlin GGUF parser
            // For now, return empty capabilities
            // This will be enhanced in STEP 10
            
            return@withContext ModelCapabilities(
                architecture = null, // Would extract from GGUF kv-pairs
                quantization = extractQuantizationFromFilename(file.name),
                parameterCount = null,
                contextLength = null,
                estimatedRamRequirementBytes = estimateRamRequirement(file.length())
            )
        } catch (e: Exception) {
            // Never throw in metadata extraction - return empty
            return@withContext ModelCapabilities(
                estimatedRamRequirementBytes = estimateRamRequirement(file.length())
            )
        }
    }
    
    override suspend fun estimateDeviceSuitability(
        capabilities: ModelCapabilities,
        fileSizeBytes: Long
    ): DeviceSuitability {
        val availableMemory = getAvailableMemory()
        val totalMemory = getTotalMemory()
        
        val estimatedRequired = capabilities.estimatedRamRequirementBytes
            ?: estimateRamRequirement(fileSizeBytes)
        
        return when {
            estimatedRequired > totalMemory * 0.8f -> {
                // Would consume more than 80% of total RAM
                DeviceSuitability.Unsupported(
                    "Model requires ~${formatBytes(estimatedRequired)}, device has only ${formatBytes(totalMemory)}"
                )
            }
            estimatedRequired > availableMemory -> {
                // Not enough available right now, but might work if other apps are closed
                DeviceSuitability.NotRecommended(
                    "Requires ${formatBytes(estimatedRequired)}, only ${formatBytes(availableMemory)} available"
                )
            }
            estimatedRequired > availableMemory * 0.7f -> {
                // Tight but might work
                DeviceSuitability.Caution(
                    "Uses ${formatBytes(estimatedRequired)} (~${(estimatedRequired * 100 / totalMemory).toInt()}% of device RAM)"
                )
            }
            else -> {
                // Plenty of room
                DeviceSuitability.Recommended
            }
        }
    }
    
    private fun getAvailableMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
    }
    
    private fun getTotalMemory(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        @Suppress("DEPRECATION")
        return memoryInfo.totalMem
    }
    
    private fun estimateRamRequirement(fileSizeBytes: Long): Long {
        // Conservative estimate: file size * 2
        // Actual requirement depends on quantization, context, etc.
        return (fileSizeBytes * RAM_MULTIPLIER).toLong()
    }
    
    private fun extractQuantizationFromFilename(filename: String): String? {
        // Heuristic: look for common quantization strings in filename
        val quantPatterns = listOf(
            "Q8_0", "Q8", "Q6_K", "Q5_K_M", "Q5_K_S", "Q5_0", "Q4_K_M", "Q4_K_S", "Q4_0",
            "Q3_K_M", "Q3_K_S", "Q2_K", "IQ4_XS", "IQ3_XXS", "GGUF"
        )
        for (pattern in quantPatterns) {
            if (filename.uppercase().contains(pattern)) {
                return pattern
            }
        }
        return null
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024L * 1024L -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f KB", bytes / 1024.0)
        }
    }
}
