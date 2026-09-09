# Raven Model Runtime Architecture

**Status**: M2.3 native runtime validation harness compiled; real-device execution blocked by missing device and external GGUF
**Last Updated**: 2026-09-04

The highest verified runtime level is **LEVEL 3 — native library packaged in the debug APK**. The JNI implementation and Android instrumentation harness are present, but no Android device/emulator or compatible external GGUF was available for real model execution. Native compilation and APK packaging are not proof of model loading or inference.

## Overview

The Raven model runtime subsystem manages local GGUF model files and provides a provider-agnostic abstraction for both local and remote language model inference.

### Core Principle

**Model files are runtime data, not source code.**

- Models are never bundled in APK/assets
- Models are stored in app-private persistent storage
- Models are acquired/imported at runtime by the user
- The runtime layer is completely separate from the domain layer

## Architecture Layers

```
┌─────────────────────────────────────────────────┐
│              Compose UI                         │
│        (Models Screen, Chat, etc.)              │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────┴──────────────────────────────┐
│         Domain Use Cases                        │
│    - ImportModelUseCase                         │
│    - SelectAndCopyModelFileUseCase              │
│    - GenerateWithProviderUseCase (future)       │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────┴──────────────────────────────┐
│         Provider Router                         │
│    Route to Local or Online provider            │
└──────┬──────────────────────────────────────┬───┘
       │                                      │
       ▼                                      ▼
┌─────────────────────────┐        ┌─────────────────────────┐
│   LocalLlmProvider      │        │  StubOnlineProvider     │
│ - Model lifecycle       │        │  (M4 implementation)    │
│ - Streaming generation  │        │  - API calls            │
└──────┬──────────────────┘        └─────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│    LocalInferenceRuntime (JNI Boundary)         │
│  - Mock (M2)                                    │
│  - llama.cpp (M2 future)                        │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
          Native llama.cpp library
          (installed via Gradle)


Data Layer
┌─────────────────────────────────────────────────┐
│          Model Repository (Room)                │
│    - Persist metadata                           │
│    - Query installed models                     │
│    - Track model lifecycle                      │
└──────────────────┬──────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
   ┌──────────┐         ┌──────────────┐
   │  ModelDao│         │ RavenDatabase│
   └──────────┘         └──────────────┘


Storage Layer
┌─────────────────────────────────────────────────┐
│   FileModelStorage                              │
│  - Copy, move, delete files                     │
│  - Calculate hashes                             │
└──────────────────┬──────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        ▼                     ▼
┌─────────────────┐   ┌──────────────┐
│ModelPathResolver│   │ Validation   │
│ - Resolve paths │   │ - Format     │
│ - Staging       │   │ - Integrity  │
└─────────────────┘   │ - Metadata   │
                      │ - Device suit│
                      └──────────────┘


Filesystem (App-Private)
<app-private>/models/
  registry/
    registry.json
  .staging/
    <temp-import-files>
  <model-id>/
    model.gguf
    metadata.json
```

## Key Components

### Domain Types

**ModelId** - Strong type for model identifiers  
**ModelStatus** - Full lifecycle: Discovered → Installing → Installed → Ready → Loading → Loaded  
**ModelCapabilities** - Model specs: architecture, quantization, context length, etc.  
**ModelMetadata** - Complete model record: ID, file info, capabilities, timestamps, status  
**ModelSource** - How model was obtained: IMPORTED, DOWNLOADED, SYSTEM  
**ModelError** - Typed error categories  

### Storage Layer

**ModelPathResolver** - Calculate paths for models, staging, registry  
**FileModelStorage** - Copy/move/delete files, hash calculation, cleanup  
- Rejects path traversal and final destination collisions

### Registry Layer

**ModelRepository** - Persistence interface  
**ModelDao** - Room database access  
**ModelEntity** - Room entity for models table  
**RavenDatabase** - Room database singleton  

### Validation Layer

**GgufValidator** - Multi-layer validation  
- Layer 1: File existence/readability
- Layer 2: Integrity (SHA-256)
- Layer 3: GGUF format (magic bytes + structure)
- Layer 4: Metadata extraction
- Layer 5: Device suitability estimation

**BasicGgufValidator** - Current implementation (stub metadata extraction)  
**DeviceSuitability** - Recommendation: Recommended | Caution | NotRecommended | Unsupported

### Import Pipeline

**SelectAndCopyModelFileUseCase**  
- Handles Android file picker (content:// URIs)
- Copies to temp location without broad permissions
- Gets display name and file size

**ImportModelUseCase**  
- Orchestrates full import pipeline:
  1. Copy to staging
  2. Validate file
  3. Validate GGUF format
  4. Verify checksum (if provided)
  5. Extract metadata
  6. Check device suitability
  7. Move to final location
  8. Register in database
- Cleans up staging files on failure

### Provider Layer

**LlmProvider** - Abstract interface for all providers  
- listModels() - List available models
- stream(request) - Stream generation events
- health() - Provider availability
- selectModel(id) - Load specific model
- unload() - Free resources

**LocalLlmProvider** - Local inference provider  
- Manages model lifecycle
- Streams generation from native runtime
- Handles model selection/unload

**StubOnlineProvider** - Placeholder for M4  

**LocalInferenceRuntime** - Native boundary  
- loadModel() - Load GGUF into memory
- generateToken() - Generate next token
- isModelLoaded() - Check state
- Interface for both Mock and llama.cpp implementations

**MockLocalInferenceRuntime** - Testing implementation

**RealNativeInferenceSmokeTest** - Android instrumentation harness
- Accepts a device-readable external GGUF through `raven.gguf.path`
- Imports through the existing staging, validation, storage, and Room registry path
- Selects through `LocalLlmProvider`
- Collects real `GenerationEvent` streaming output
- Exercises unload and reload
- Compiles, but has not executed without a device and external GGUF

### Provider Router

**ProviderRouter** - Routes requests to active provider  
- Register providers
- Select active provider
- Get provider by ID
- Check provider health
- List available providers

## Model Lifecycle

```
User selects file via Android file picker
          │
          ▼
SelectAndCopyModelFileUseCase
          │
          ├─ Get URI from file picker
          ├─ Query display name + size
          └─ Copy to temp file
          │
          ▼
ImportModelUseCase.importModel()
          │
          ├─ Copy to .staging/
          ├─ Validate file exists
          ├─ Validate GGUF format (magic bytes)
          ├─ Validate checksum (if provided)
          ├─ Extract metadata
          ├─ Estimate device suitability
          ├─ Move to models/<id>/
          ├─ Register in Room database
          └─ Status: Installed
          │
          ▼
User selects model from Models list
          │
          ▼
LocalLlmProvider.selectModel(modelId)
          │
          ├─ Unload previous model (if any)
          ├─ LocalInferenceRuntime.loadModel()
          └─ Status: Ready → Loading → Loaded
          │
          ▼
Conversation Engine sends GenerationRequest
          │
          ▼
ProviderRouter.getSelectedProvider().stream()
          │
          ├─ LocalLlmProvider.stream()
          ├─ Emit: Started
          ├─ Generate tokens one at a time
          ├─ Emit: Token(text) for each token
          ├─ Support cancellation via coroutine
          └─ Emit: Completed or Failed
```

## Storage Layout

```
<android:filesDir>/models/

registry/
  registry.json          # Model registry (JSON list of ModelEntity)

.staging/
  <uuid-1>              # Import in progress
  <uuid-2>              # Download in progress

gemma-7b-q4/
  model.gguf            # The actual model file
  metadata.json         # Extracted capabilities (future)

qwen-7b-q5/
  model.gguf
  metadata.json
```

## Import Process - Detailed

### Input
- `sourceFile: File` - from user's file picker or download
- `expectedSha256: String?` - optional expected checksum

### Stages

**Stage 1: Copy to Staging**  
- Create staging directory if needed
- Copy file to `.staging/<uuid>`
- File is not in use, safe to validate/move

**Stage 2: Validate File**  
- Check file exists and is readable
- Check file is not empty
- Check minimum size for GGUF

**Stage 3: Validate GGUF Format**  
- Read first 4 bytes: must be "GGUF" (0x47 0x47 0x55 0x46)
- Minimal structure check

**Stage 4: Validate Checksum**  
- If expected checksum provided:
  - Calculate SHA-256 of file
  - Compare to expected
  - Throw on mismatch

**Stage 5: Extract Metadata**  
- Parse GGUF kv-pairs (future: requires llama.cpp)
- For now: estimate from filename + heuristics

**Stage 6: Check Device Suitability**  
- Get device total/available memory
- Estimate RAM requirement
- Return recommendation (Recommended | Caution | NotRecommended | Unsupported)
- Note: doesn't block import, just informs user

**Stage 7: Move from Staging**  
- Create model directory: `models/<model-id>/`
- Move file from staging to `models/<model-id>/model.gguf`

**Stage 8: Register in Database**  
- Create ModelEntity
- Insert into Room database
- Set status: Installed

### Error Handling

If any stage fails:
1. Delete staging file (cleanup)
2. Throw appropriate ModelError (typed)
3. Don't register in database
4. Model never marked as installed

Example errors:
- `ModelError.InvalidFile` - not a valid file
- `ModelError.InvalidFormat` - not valid GGUF
- `ModelError.ChecksumMismatch` - hash doesn't match
- `ModelError.InsufficientStorage` - not enough disk space
- `ModelError.InsufficientMemory` - too large for device

## Device Suitability Estimation

### Inputs
- Model file size (bytes)
- Model capabilities (if metadata available)
- Device total RAM
- Device available RAM
- Device ABI

### Heuristic (Current)

```
estimatedRam = fileSize * 2.0f  // Conservative multiplier

if estimatedRam > totalRam * 0.8:
    return Unsupported
elif estimatedRam > availableRam:
    return NotRecommended
elif estimatedRam > availableRam * 0.7:
    return Caution
else:
    return Recommended
```

### Future Enhancement
- Parse GGUF metadata: param count, quantization, context length
- Query device capabilities: RAM tier, ABI, CPU features
- More precise recommendation

## Session/Model Lifecycle (Native Boundary)

```
Before:   NOT_LOADED
  │
  ├─ User taps "Use Model"
  ├─ LocalLlmProvider.selectModel(modelId)
  │
  ▼
After:    LOADING
  │
  ├─ LocalInferenceRuntime.loadModel(metadata)
  ├─ JNI → llama.cpp loads GGUF
  ├─ Allocates GPU/CPU memory
  ├─ Prepares context
  │
  ▼
After:    LOADED
  │
  ├─ User sends message
  ├─ Conversation engine calls provider.stream()
  ├─ LocalLlmProvider streams generation
  │
  ├─ LocalInferenceRuntime.generateToken() repeatedly
  │   (returns one token per call)
  │   (Emitted as GenerationEvent.Token)
  │
  ├─ User presses "Stop"
  │   (Flow is cancelled via coroutine)
  │
  ├─ Emit: GenerationEvent.Cancelled or Completed
  │
  ▼
After:    LOADED (ready for next generation)
  │
  ├─ User selects different model
  ├─ LocalLlmProvider.unload()
  │
  ▼
After:    NOT_LOADED
  │
  └─ Memory freed, ready for new model
```

## Provider Switching

**Key Property**: Conversation state is NOT tied to provider.

```
User on Local Provider:
  Message 1: "What is AI?"
  Memory: [facts about AI]
  Response: Local model generates response

User switches to Online:
  → ProviderRouter.selectProvider("online")
  → Same conversation
  → Same memory
  → Next message uses Online provider
  → History preserved

User switches back to Local:
  → ProviderRouter.selectProvider("local")
  → Same conversation
  → Same memory
  → Next message uses Local provider
```

Implementation:
- ConversationEngine never knows which provider it's using
- Only ProviderRouter knows about provider identity
- Message history is provider-agnostic
- Memory is provider-agnostic
- Switching is just swapping the LlmProvider reference

## Testing Strategy

### Unit Tests (JVM)
- Model domain types (ModelId, ModelStatus, etc.)
- ModelRepository with mock DAO
- GgufValidator with stub runtime
- ProviderRouter selection logic
- ImportModelUseCase orchestration (mocked storage)

### Integration Tests (Android)
- FileModelStorage with real filesystem
- ModelRepository with real Room database
- SelectAndCopyModelFileUseCase with ContentResolver
- End-to-end import flow

### Mock Providers
- MockLocalInferenceRuntime - generates mock tokens
- StubOnlineProvider - placeholder until M4

## Known Limitations & TODOs

### M2 (Current)
- [x] Domain types for model lifecycle
- [x] Storage abstraction and implementation
- [x] Room registry
- [x] GGUF format validation (magic bytes only)
- [x] Metadata extraction (stub - filename heuristics)
- [x] Device suitability estimation (heuristic only)
- [x] Android file import (ContentResolver)
- [x] Provider abstraction
- [x] Local provider skeleton
- [x] Provider router
- [x] JVM storage/import/registry tests (24 executed, 0 failures)
- [ ] Android instrumentation tests (device/emulator not available)
- [ ] Integration tests with real database
- [ ] Instrumentation tests on device/emulator

### M3 (Memory)
- Conversation storage (already designed)
- Memory extraction/retrieval
- Memory viewer UI

### M4 (Online Provider)
- OpenAI-compatible HTTP adapter
- Streaming HTTP responses
- Error mapping
- API key secure storage
- Provider configuration UI

### Future (Beyond V1)
- Full GGUF metadata extraction (requires llama.cpp integration)
- Model benchmarking on device
- Device-specific model recommendations
- Model quantization profiler
- Download resume/retry
- Model versioning/updates
- Cache optimization

## llama.cpp Integration (STEP 10)

When integrating actual llama.cpp:

1. Add llama.cpp JNI dependency to Gradle
2. Implement real LocalInferenceRuntime wrapping JNI
3. Load GGUF context in loadModel()
4. Stream tokens in generateToken() via JNI callback
5. Handle OOM exceptions gracefully
6. Implement cancellation via native API
7. Test on real device with small models first

## Security Considerations

1. **Model File Integrity**
   - SHA-256 verification (if expected hash provided)
   - Never execute untrusted code from model

2. **Storage**
   - App-private storage (only this app can access)
   - No world-readable model files
   - Staging files cleaned up after import

3. **Permissions**
   - READ_EXTERNAL_STORAGE for user-selected files (SAF/ContentResolver)
   - INTERNET for future online provider
   - No unnecessary permissions

4. **Secrets**
   - API keys stored securely (encrypted SharedPreferences/Keystore)
   - Never logged
   - Never sent to local model

## Performance Notes

1. **File Copy/Import**
   - Runs on IO dispatcher
   - Doesn't block UI thread
   - Progress can be reported

2. **Model Loading**
   - Runs on Default dispatcher
   - Should show loading indicator
   - First-time load may take 30+ seconds (model parsing + memory allocation)

3. **Generation**
   - Native thread in llama.cpp
   - Emits events to Flow (non-blocking)
   - Cancellable via coroutine

4. **Memory Management**
   - Only one model loaded at a time (by design)
   - Unload before loading new model
   - Monitor heap before loading large models
