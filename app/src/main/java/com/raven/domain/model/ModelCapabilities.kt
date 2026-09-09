package com.raven.domain.model

/**
 * Represents the capabilities and specifications of a model.
 * Some fields may be unknown if metadata extraction failed.
 */
data class ModelCapabilities(
    /** Model architecture (e.g., "llama", "mistral", "phi") */
    val architecture: String? = null,
    
    /** Quantization method (e.g., "Q4_K_M", "Q5_K_M") */
    val quantization: String? = null,
    
    /** Total parameter count (in millions, typically) */
    val parameterCount: Long? = null,
    
    /** Context length the model was trained with */
    val contextLength: Int? = null,
    
    /** Embedding dimension */
    val embeddingDim: Int? = null,
    
    /** Vocabulary size */
    val vocabularySize: Int? = null,
    
    /** Model family (e.g., "Gemma", "Qwen", "Llama") */
    val modelFamily: String? = null,
    
    /** Training data cutoff date if known */
    val trainingDataCutoff: String? = null,
    
    /** Estimated RAM requirement in bytes (for this device) */
    val estimatedRamRequirementBytes: Long? = null,
) {
    fun hasMetadata(): Boolean = architecture != null || quantization != null || parameterCount != null
    
    fun displayCapabilities(): String {
        val parts = mutableListOf<String>()
        architecture?.let { parts.add("$it") }
        quantization?.let { parts.add(it) }
        parameterCount?.let { parts.add("${it}M params") }
        contextLength?.let { parts.add("${it} ctx") }
        return parts.joinToString(" • ")
    }
}
