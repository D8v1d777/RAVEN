# Project Map

```text
Raven/
├── README.md
├── PROJECT_MAP.md
│
├── docs/
│   ├── PROJECT_CONTRACT.md
│   ├── DEFINITION_OF_DONE.md
│   ├── ARCHITECTURE.md
│   ├── CURRENT_STATUS.md ← M2.3 validation status
│   ├── MODEL_RUNTIME_ARCHITECTURE.md ← NEW (M2 design docs)
│   ├── MILESTONES.md
│   ├── FUTURE.md
│   └── MODEL_POLICY.md
│
├── claude/
│   ├── CLAUDE.md
│   ├── TASK_PROTOCOL.md
│   ├── CURRENT_STATUS.md ← UPDATED
│   └── FIRST_PROMPT.md
│
├── contracts/
│   ├── LLM_PROVIDER_CONTRACT.md
│   ├── MEMORY_CONTRACT.md
│   └── PERSONA_CONTRACT.md
│
├── checklists/
│   └── V1_ACCEPTANCE.md
│
├── resources/
│   └── RESOURCES.md
│
├── scaffold/
│   ├── DOMAIN_TYPES.kt
│   ├── LLM_PROVIDER.kt
│   └── RUNTIME_BOUNDARIES.md
│
└── app/
    ├── build.gradle ← Updated with permissions
    ├── src/
    │   ├── main/
    │   │   ├── AndroidManifest.xml ← Updated permissions
    │   │   ├── java/com/raven/
    │   │   │   ├── MainActivity.kt
    │   │   │   ├── domain/
    │   │   │   │   ├── model/
    │   │   │   │   │   ├── ModelId.kt ← NEW
    │   │   │   │   │   ├── ModelStatus.kt ← NEW
    │   │   │   │   │   ├── ModelSource.kt ← NEW
    │   │   │   │   │   ├── ModelCapabilities.kt ← NEW
    │   │   │   │   │   ├── ModelMetadata.kt ← NEW
    │   │   │   │   │   ├── ModelError.kt ← NEW
    │   │   │   │   │   ├── repository/
    │   │   │   │   │   │   └── ModelRepository.kt ← NEW
    │   │   │   │   │   └── usecase/
    │   │   │   │   │       ├── ImportModelUseCase.kt ← NEW
    │   │   │   │   │       └── SelectAndCopyModelFileUseCase.kt ← NEW
    │   │   │   │   ├── chat/
    │   │   │   │   ├── memory/
    │   │   │   │   └── settings/
    │   │   │   ├── data/
    │   │   │   │   ├── db/
    │   │   │   │   │   └── RavenDatabase.kt ← NEW
    │   │   │   │   ├── model/
    │   │   │   │   │   ├── db/
    │   │   │   │   │   │   ├── ModelEntity.kt ← NEW
    │   │   │   │   │   │   └── ModelDao.kt ← NEW
    │   │   │   │   │   ├── storage/
    │   │   │   │   │   │   ├── ModelPathResolver.kt ← NEW
    │   │   │   │   │   │   ├── AndroidModelPathResolver.kt ← NEW
    │   │   │   │   │   │   └── FileModelStorage.kt ← NEW
    │   │   │   │   │   ├── validation/
    │   │   │   │   │   │   ├── GgufValidator.kt ← NEW
    │   │   │   │   │   │   └── BasicGgufValidator.kt ← NEW
    │   │   │   │   │   └── repository/
    │   │   │   │   │       └── RoomModelRepository.kt ← NEW
    │   │   │   │   └── local/ (future: memory, remote)
    │   │   │   ├── inference/
    │   │   │   │   ├── provider/
    │   │   │   │   │   └── LlmProvider.kt ← UPDATED (enhanced contract)
    │   │   │   │   ├── local/
    │   │   │   │   │   ├── LocalLlmProvider.kt ← NEW
    │   │   │   │   │   ├── LocalInferenceRuntime.kt ← NEW (JNI boundary)
    │   │   │   │   │   └── MockLocalInferenceRuntime.kt ← NEW
    │   │   │   │   ├── online/
    │   │   │   │   │   └── StubOnlineProvider.kt ← NEW (M4 placeholder)
    │   │   │   │   └── router/
    │   │   │   │       └── ProviderRouter.kt ← NEW
    │   │   │   ├── feature/
    │   │   │   │   ├── chat/
    │   │   │   │   ├── models/ (future: UI)
    │   │   │   │   ├── memory/
    │   │   │   │   └── settings/
    │   │   │   └── ui/
    │   │   │       ├── theme/
    │   │   │       └── nav/
    │   │   └── res/
    │   └── test/
    │       └── java/com/raven/
    │           └── domain/
    │               └── model/
    │                   └── ModelDomainTypesTest.kt ← NEW (unit tests)
    └── build/ (generated)
```

## Implementation Status by Layer

### Domain Layer (100%)
- ✅ Model types (Id, Status, Source, Capabilities, Metadata, Error)
- ✅ Repository interface
- ✅ Use cases (Import, File picker integration)

### Data Layer (100%)
- ✅ Room entities and DAO
- ✅ Database singleton
- ✅ Repository implementation
- ✅ Storage abstraction and implementation
- ✅ Validation (multi-layer)

### Inference Layer (70%)
- ✅ Provider interface (enhanced)
- ✅ Local provider (model lifecycle and streaming boundary)
- ✅ Provider router
- ✅ Mock runtime for testing
- ✅ llama.cpp JNI runtime and Android native library packaging
- ✅ Real GGUF smoke harness compiles
- ⏳ Real GGUF/device runtime execution
- ⏳ Online provider HTTP adapter (M4)

### UI Layer (0%)
- ⏳ Models screen
- ⏳ Import dialog
- ⏳ Model selection

### Testing (35%)
- ✅ Unit tests for domain types
- ⏳ Integration tests (Room + filesystem)
- ✅ RealNativeInferenceSmokeTest compiles
- ⏳ Instrumentation tests on device/emulator with external GGUF

## Key Design Patterns

| Layer | Pattern | Location |
|-------|---------|----------|
| Domain | Strong types, sealed classes | `domain/model/` |
| Data | Repository pattern, DAO | `data/model/repository/` |
| Storage | Strategy pattern, abstraction | `data/model/storage/` |
| Validation | Pipeline/chain | `data/model/validation/` |
| Inference | Provider/adapter pattern | `inference/provider/` |
| Lifecycle | State machine | `ModelStatus` enum |

## File Organization Principles

1. **Package by feature**: `model/`, `chat/`, `memory/`, `settings/`
2. **Layer separation**: `domain/`, `data/`, `inference/`, `ui/`, `feature/`
3. **Abstraction-first**: Interfaces before implementations
4. **No cross-layer leaks**: UI never imports `inference/local/`, `data/`, storage, validation, native
5. **Testing close to source**: Tests in parallel package structure

## Buildable State

✅ **Current state**: Debug Kotlin, native compilation, lint, and clean APK assembly pass  
⚠️ **Gradle**: Build works with the bundled SDK/JDK; offline test dependencies are not cached  
✅ **Tests**: 24 JVM tests executed with 0 failures; instrumentation pending  
⏳ **Integration**: Needs Room database in-memory for testing  
⏳ **Instrumentation**: Harness is ready; device/emulator and external GGUF are required  

## Next Milestones

- **M2 Continuation**: Finish tests, integrate llama.cpp
- **M3 Memory**: Add conversation/memory persistence  
- **M4 Online**: HTTP provider + configuration  
- **M5 Persona**: Raven character + system prompt  
- **M6 Polish**: UI/UX, animations, release

