# LLM Provider Contract

## Purpose

Define one stable interface for local and online generation.

## Required operations

```kotlin
interface LlmProvider {
    val id: String
    suspend fun listModels(): List<ModelDescriptor>
    fun stream(request: GenerationRequest): Flow<GenerationEvent>
    suspend fun health(): ProviderHealth
}
```

## GenerationRequest

Must contain:
- modelId
- systemPrompt
- ordered messages
- generation options
- memory context
- optional stop sequences

## GenerationEvent

Suggested variants:
- Started
- Token(text)
- Completed(usage)
- Cancelled
- Failed(error)

## Provider requirements

Every provider must:
- support cancellation as far as possible;
- surface typed errors;
- never mutate conversation history directly;
- never write secrets to logs;
- return provider/model identity with completed generations when available.

## Local provider

`LlamaCppProvider` is responsible for:
- GGUF model loading;
- native runtime lifecycle;
- token streaming;
- cancellation;
- model metadata;
- memory/resource safety checks.

## Online provider

`OpenAiCompatibleProvider` is responsible for:
- HTTPS requests;
- authentication;
- request serialization;
- streaming response parsing;
- HTTP error mapping;
- endpoint/model configuration.

A future `GeminiProvider` may implement the same interface. Do not make the interface OpenAI-specific.
