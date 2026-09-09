# Raven Architecture

## High-level design

```text
Compose UI
   |
   v
Presentation/ViewModels
   |
   v
Conversation Orchestrator
   |---------------------------|
   v                           v
Provider Router             Memory Service
   |                           |
   +-----------+               v
   |           |             Room/SQLite
Offline      Online
   |           |
llama.cpp   API Adapter
   |
GGUF model file
```

## Architectural rule

UI must know only about application state and user actions. It must not know how llama.cpp, HTTP, JSON, GGUF files, Room, or secure key storage works.

## Suggested modules/packages

```text
com.raven
├── app
├── core
│   ├── model
│   ├── result
│   ├── security
│   └── logging
├── data
│   ├── local
│   ├── remote
│   ├── repositories
│   └── preferences
├── domain
│   ├── chat
│   ├── memory
│   ├── model
│   └── settings
├── inference
│   ├── api
│   ├── offline
│   └── online
└── feature
    ├── chat
    ├── models
    ├── memory
    └── settings
```

## Provider abstraction

The domain depends on this conceptual contract:

```kotlin
interface LlmProvider {
    val id: String
    suspend fun listModels(): List<ModelDescriptor>
    fun stream(request: GenerationRequest): Flow<GenerationEvent>
    suspend fun health(): ProviderHealth
}
```

The exact implementation should remain in the project code, but the contract above is the boundary that prevents provider lock-in.

## Provider router

```text
User selects mode
       |
       v
ProviderRouter
  |          |
LOCAL      ONLINE
  |          |
LlamaCpp   HttpApiAdapter
```

The router must reject an unavailable provider with a domain-level error; it must not silently fall back from Online to Local or vice versa.

## Memory architecture

V1 intentionally uses ordinary SQLite/Room tables instead of adding a vector database.

Use a simple retrieval pipeline:

```text
Current user message
       |
       v
keyword/FTS retrieval of memory candidates
       |
       v
importance + recency ranking
       |
       v
small memory context block
       |
       v
LLM generation
```

A future semantic embedding layer can replace the retrieval implementation behind the same `MemoryRepository` interface.

## Database entities

### Conversation
- id
- title
- createdAt
- updatedAt

### Message
- id
- conversationId
- role
- content
- providerId
- modelId
- createdAt
- generationMetadataJson nullable

### Memory
- id
- kind
- content
- importance
- createdAt
- updatedAt
- lastUsedAt nullable
- sourceMessageId nullable
- userApproved

### ModelProfile
- id
- displayName
- localPath/URI reference
- format
- quantization nullable
- parameterCount nullable
- sizeBytes
- providerId
- isDefault

### ProviderProfile
- id
- type
- displayName
- endpoint
- modelName
- apiKey reference/alias only
- enabled

Never store raw API keys in Room.

## Local model file handling

Use Android's Storage Access Framework for user-selected model files. Prefer copying/importing into app-managed storage when practical so the runtime has stable access. Do not assume a filesystem path from a content URI.

## Concurrency rules

Only one generation may be active for a conversation in v1. The UI may display an active generation and offer cancellation. Provider implementations must support cancellation as far as the underlying runtime permits.

## Failure boundaries

The following errors must be explicit:
- ModelNotFound
- ModelLoadFailed
- OutOfMemoryRisk
- ProviderUnavailable
- AuthenticationFailed
- RateLimited
- NetworkUnavailable
- GenerationCancelled
- GenerationFailed
- InvalidModelFile

Do not turn all failures into a generic "Something went wrong" state internally.

## Logging

Use structured local logs only. Never log:
- API keys
- authorization headers
- full sensitive memory records
- complete prompts by default

Logs should support debugging provider selection, model loading, latency, token counts where available, and failures.
