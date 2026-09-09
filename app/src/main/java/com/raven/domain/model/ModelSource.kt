package com.raven.domain.model

/**
 * Represents how a model was obtained.
 * Allows future support for multiple sources without hard-coding one.
 */
enum class ModelSource {
    /** User imported via Android file picker */
    IMPORTED,
    
    /** User downloaded from a remote catalog */
    DOWNLOADED,
    
    /** System-provided model (future) */
    SYSTEM,
    
    /** Unknown/legacy source */
    UNKNOWN;
    
    companion object {
        fun fromString(value: String): ModelSource = 
            try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
    }
}
