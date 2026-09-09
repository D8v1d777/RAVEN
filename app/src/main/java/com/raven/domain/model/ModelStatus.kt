package com.raven.domain.model

/**
 * Lifecycle states for a model.
 * Enables the system to distinguish between:
 * - ongoing operations (downloading, importing, validating)
 * - ready states (installed, loaded)
 * - failure states (with categorization)
 */
sealed class ModelStatus {
    /** Model exists in registry but is in an unknown state */
    data object Discovered : ModelStatus()

    /** Model file is being downloaded from remote source */
    data class Downloading(val progressBytes: Long, val totalBytes: Long) : ModelStatus()

    /** Model file is being imported (copied to staging) */
    data object Importing : ModelStatus()

    /** Model file is being verified for integrity/format */
    data object Verifying : ModelStatus()

    /** Model is fully installed and available for selection */
    data object Installed : ModelStatus()

    /** Model is installed and ready to load for generation */
    data object Ready : ModelStatus()

    /** Model is being loaded into memory */
    data object Loading : ModelStatus()

    /** Model is fully loaded and can generate tokens */
    data object Loaded : ModelStatus()

    // Failure states - categorized by failure type
    data class DownloadFailed(val reason: String) : ModelStatus()
    data class ImportFailed(val reason: String) : ModelStatus()
    data class ValidationFailed(val reason: String) : ModelStatus()
    data class InvalidFormat(val reason: String) : ModelStatus()
    data class ChecksumFailed(val expected: String?, val actual: String) : ModelStatus()
    data class UnsupportedModel(val reason: String) : ModelStatus()
    data class InsufficientStorage(val requiredBytes: Long, val availableBytes: Long) : ModelStatus()
    data class InsufficientMemory(val requiredBytes: Long, val availableBytes: Long) : ModelStatus()
    data class LoadFailed(val reason: String) : ModelStatus()

    fun isReady(): Boolean = this is Ready
    fun isLoaded(): Boolean = this is Loaded
    fun isError(): Boolean = this is DownloadFailed || this is ImportFailed || 
            this is ValidationFailed || this is InvalidFormat || this is ChecksumFailed ||
            this is UnsupportedModel || this is InsufficientStorage || 
            this is InsufficientMemory || this is LoadFailed

    fun userMessage(): String = when (this) {
        is Discovered -> "Discovered"
        is Downloading -> "Downloading (${progressBytes}B / ${totalBytes}B)"
        is Importing -> "Importing"
        is Verifying -> "Verifying"
        is Installed -> "Installed"
        is Ready -> "Ready"
        is Loading -> "Loading"
        is Loaded -> "Loaded"
        is DownloadFailed -> "Download failed: $reason"
        is ImportFailed -> "Import failed: $reason"
        is ValidationFailed -> "Validation failed: $reason"
        is InvalidFormat -> "Invalid format: $reason"
        is ChecksumFailed -> "Checksum mismatch. Expected: $expected, Got: $actual"
        is UnsupportedModel -> "Unsupported: $reason"
        is InsufficientStorage -> "Insufficient storage: need ${requiredBytes}B, have ${availableBytes}B"
        is InsufficientMemory -> "Insufficient memory: need ${requiredBytes}B, have ${availableBytes}B"
        is LoadFailed -> "Load failed: $reason"
    }
}
