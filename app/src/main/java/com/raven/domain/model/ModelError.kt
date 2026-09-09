package com.raven.domain.model

/**
 * Typed errors for model operations.
 * Prevents leaking platform-specific exceptions into the UI.
 */
sealed class ModelError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    data class NotFound(
        val modelId: ModelId,
        override val message: String = "Model not found: $modelId"
    ) : ModelError(message)

    data class InvalidFile(
        override val message: String = "Invalid model file"
    ) : ModelError(message)

    data class ChecksumMismatch(
        val expected: String,
        val actual: String,
        override val message: String = "Checksum mismatch: expected $expected, got $actual"
    ) : ModelError(message)

    data class InsufficientStorage(
        val requiredBytes: Long,
        val availableBytes: Long,
        override val message: String = "Insufficient storage: need $requiredBytes bytes, have $availableBytes bytes"
    ) : ModelError(message)

    data class InsufficientMemory(
        val requiredBytes: Long,
        val availableBytes: Long,
        override val message: String = "Insufficient memory: need $requiredBytes bytes, have $availableBytes bytes"
    ) : ModelError(message)

    data class UnsupportedModel(
        override val message: String = "Model not supported on this device"
    ) : ModelError(message)

    data class ModelLoadFailed(
        override val message: String = "Failed to load model",
        override val cause: Throwable? = null
    ) : ModelError(message, cause)

    data class GenerationFailed(
        override val message: String = "Generation failed",
        override val cause: Throwable? = null
    ) : ModelError(message, cause)

    data class CopyFailed(
        override val message: String = "Failed to copy model file",
        override val cause: Throwable? = null
    ) : ModelError(message, cause)

    data class ValidationFailed(
        override val message: String = "Model validation failed",
        override val cause: Throwable? = null
    ) : ModelError(message, cause)

    data class ImportFailed(
        override val message: String,
        override val cause: Throwable? = null
    ) : ModelError(message, cause)
}
