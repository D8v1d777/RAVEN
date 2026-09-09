package com.raven.data.model.validation

import com.raven.domain.model.ModelError
import com.raven.domain.model.ModelCapabilities
import java.io.File

/**
 * Validates GGUF model files in stages.
 * Layer 1: File existence/readability
 * Layer 2: Integrity (checksum if available)
 * Layer 3: GGUF format validation
 * Layer 4: Metadata extraction
 * Layer 5: Device suitability estimation
 */
interface GgufValidator {
    /**
     * Validate file exists and is readable.
     * @throws ModelError.InvalidFile if validation fails
     */
    suspend fun validateFileExists(file: File)
    
    /**
     * Validate checksum if expected value is provided.
     * @throws ModelError.ChecksumMismatch if mismatch detected
     */
    suspend fun validateChecksum(
        file: File,
        expectedSha256: String
    )
    
    /**
     * Validate file is a valid GGUF format.
     * Checks magic bytes and basic structure.
     * @throws ModelError.InvalidFile if not valid GGUF
     */
    suspend fun validateGgufFormat(file: File)
    
    /**
     * Extract metadata from GGUF file.
     * Returns partial metadata even if some fields are unavailable.
     * Never throws - returns empty capabilities if extraction fails.
     */
    suspend fun extractMetadata(file: File): ModelCapabilities
    
    /**
     * Estimate device suitability.
     * Returns a recommendation based on device resources.
     */
    suspend fun estimateDeviceSuitability(
        capabilities: ModelCapabilities,
        fileSizeBytes: Long
    ): DeviceSuitability
}

sealed class DeviceSuitability {
    data object Recommended : DeviceSuitability()
    data class Caution(val reason: String) : DeviceSuitability()
    data class NotRecommended(val reason: String) : DeviceSuitability()
    data class Unsupported(val reason: String) : DeviceSuitability()
}
