package com.raven.domain.model

/**
 * Strong type for a model identifier.
 * Prevents accidental mix-up with other string IDs.
 * Stable across app restarts and provider selections.
 */
@JvmInline
value class ModelId(val value: String) {
    init {
        require(value.isNotBlank()) { "ModelId cannot be blank" }
        require(value.matches(Regex("^[a-zA-Z0-9._-]+$"))) { "ModelId must be alphanumeric with . _ - only" }
    }

    override fun toString(): String = value
}
