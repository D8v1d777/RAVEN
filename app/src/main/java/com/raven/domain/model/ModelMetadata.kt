package com.raven.domain.model

/**
 * Complete metadata for a locally managed model.
 * Persisted in the model registry and used throughout the app.
 */
data class ModelMetadata(
    /** Unique identifier for this model */
    val id: ModelId,
    
    /** Display name shown to user */
    val displayName: String,
    
    /** File name on disk */
    val fileName: String,
    
    /** File size in bytes */
    val fileSizeBytes: Long,
    
    /** Current status/lifecycle state */
    val status: ModelStatus = ModelStatus.Discovered,
    
    /** How the model was obtained */
    val source: ModelSource = ModelSource.UNKNOWN,
    
    /** Model capabilities and specifications */
    val capabilities: ModelCapabilities = ModelCapabilities(),
    
    /** SHA-256 hash of the file (for integrity verification) */
    val sha256: String? = null,
    
    /** Expected SHA-256 from source (e.g., catalog) */
    val expectedSha256: String? = null,
    
    /** Timestamp when model was installed */
    val installedAtEpochMs: Long = System.currentTimeMillis(),
    
    /** Timestamp when model was last used */
    val lastUsedAtEpochMs: Long? = null,
    
    /** Full license text if available */
    val licenseText: String? = null,
    
    /** Download/source URL if applicable */
    val sourceUrl: String? = null,
    
    /** Provider that owns this model (e.g., "local", "openai") */
    val providerId: String = "local",
) {
    init {
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
        require(fileName.isNotBlank()) { "File name cannot be blank" }
        require(fileSizeBytes > 0) { "File size must be positive" }
    }
    
    fun isValid(): Boolean = status is ModelStatus.Ready || status is ModelStatus.Loaded
    fun isInstalled(): Boolean = status !is ModelStatus.DownloadFailed && 
                                 status !is ModelStatus.ImportFailed && 
                                 status !is ModelStatus.ValidationFailed &&
                                 status !is ModelStatus.Discovered
}
