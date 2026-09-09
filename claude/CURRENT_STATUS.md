# Raven Current Status

Last updated: 2026-09-04 (M2.3 runtime validation harness)

## Overall

35% — M2 storage/import foundation hardened and verified; debug unit tests pass

## Milestones

- [x] M0 Foundation (basic Android project)
- [x] M1 Mock chat (scaffolding)
- [x] M2 Offline model runtime (CURRENT - domain/storage/validation complete)
- [ ] M2 Continuation (real GGUF/device validation)
- [ ] M3 Memory
- [ ] M4 Online provider
- [ ] M5 Raven persona
- [ ] M6 Polish + release

## Current Task

M2: Model Runtime - Testing and validation phase

### Completed (M2 - Phase 1)

**Domain Layer**
- ✅ ModelId strong type with validation
- ✅ ModelStatus enum (14 states) with user-friendly messages
- ✅ ModelSource enum (IMPORTED, DOWNLOADED, SYSTEM)
- ✅ ModelCapabilities (arch, quantization, params, context, RAM estimate)
- ✅ ModelMetadata complete record type
- ✅ ModelError typed error hierarchy
- ✅ GenerationEvent and related types (from scaffold)
- ✅ LlmProvider interface contract (enhanced)

**Storage Layer**
- ✅ ModelPathResolver interface (paths, storage queries)
- ✅ AndroidModelPathResolver implementation (StatFs, filesDir)
- ✅ ModelStorage interface (copy, move, delete, hash)
- ✅ FileModelStorage implementation (all operations)
- ✅ Staging names and final model paths are constrained to managed storage
- ✅ Existing destination collisions are rejected safely

**Registry Layer**
- ✅ ModelEntity (Room entity, 1:1 mapping with domain)
- ✅ ModelDao (queries, updates, flow observation)
- ✅ RavenDatabase (Room singleton)
- ✅ ModelRepository interface (abstraction)
- ✅ RoomModelRepository implementation
- ✅ Model lifecycle status round-trips through the Room entity mapping

**Validation Layer**
- ✅ GgufValidator interface (5-layer validation)
- ✅ BasicGgufValidator implementation
  - Layer 1: File existence/readability
  - Layer 2: Checksum (SHA-256)
  - Layer 3: GGUF format (magic bytes)
  - Layer 4: Metadata extraction (stub - filename heuristics)
  - Layer 5: Device suitability (RAM estimation)

**Import Pipeline**
- ✅ SelectAndCopyModelFileUseCase (Android file picker + ContentResolver)
- ✅ ImportModelUseCase (full staging → validation → installation pipeline)
- ✅ Staging directory handling
- ✅ Error handling with cleanup

**Provider Layer**
- ✅ LlmProvider interface (full contract)
- ✅ LocalLlmProvider (model selection, lifecycle, streaming mock)
- ✅ LocalInferenceRuntime boundary interface
- ✅ MockLocalInferenceRuntime (testing)
- ✅ StubOnlineProvider (placeholder for M4)
- ✅ ProviderRouter (registration, selection, health)

**Native Runtime**
- ✅ LlamaCppInferenceRuntime uses Raven-managed `filesDir/models/<model-id>/model.gguf` paths
- ✅ JNI load, prompt processing, token generation, cancellation flag, unload, and reload boundary compiled and packaged
- ✅ RealNativeInferenceSmokeTest compiles and exercises import → registry → provider → native generation when supplied a device and GGUF
- ⏳ Real Android model load/generation not executed: no connected device/emulator or external GGUF is available

**Testing**
- ✅ Domain test source exists (ModelId, Status, Capabilities, Metadata)
- ✅ Storage behavior tests (temporary files, staging, isolation, deletion, traversal)
- ✅ Import pipeline tests (success, validation failure, storage failure, collision)
- ✅ Registry mapping tests (installed/loading/failure status round-trip)
- ✅ 28 JVM tests executed with 0 failures and 0 errors
- ⏳ Android instrumentation tests require a connected device/emulator

**Documentation**
- ✅ MODEL_RUNTIME_ARCHITECTURE.md (comprehensive 400+ lines)
- ✅ Architecture diagrams
- ✅ Component descriptions
- ✅ Lifecycle documentation
- ✅ Testing strategy
- ✅ Known limitations

**Configuration**
- ✅ AndroidManifest.xml permissions (READ_EXTERNAL_STORAGE, INTERNET)
- ✅ Windows `local.properties` points to the bundled Android SDK
- ✅ Kotlin compilation passes with the bundled Gradle/JDK/SDK
- ⏳ Full test execution requires network access or cached test dependencies

### Not Yet Verified (M2.3)

**Real Runtime Validation**
- Real GGUF load and generation on Android
- Streaming/EOS behavior on a real model
- Cancellation and unload/reload on a real device
- GGUF kv-pair metadata extraction remains a separate limitation

**UI Components**
- Models screen
- Import dialog
- Model selection
- Device suitability warning display
- Loading indicators

**Testing**
- Instrumentation tests on device/emulator
- Real model file validation tests
- End-to-end import flow test
- Provider switching test

### Blockers

- ✅ Standalone compile, lint, JVM tests, and debug assembly pass with the bundled JDK 17.
- ✅ The JNI bridge contract and the remaining storage test regression were corrected and verified.
- The repository has no `.git` directory, so Git history/status cannot be inspected here.

### Assumptions Made

1. llama.cpp will be integrated via Gradle dependency (NNAPI or similar)
2. Mock runtime acceptable for initial testing
3. Staged filesystem approach is acceptable (not cache-only)
4. Room is sufficient for model registry (no vector DB needed for M2)
5. Android 21+ (minSdk=21) is acceptable

### Design Decisions

| Decision | Rationale |
|----------|-----------|
| Strong `ModelId` type | Prevents accidental ID mix-ups; enables refactoring |
| Separate storage/registry layers | Clean separation of concerns; testable |
| 5-layer validation | Catches errors early; user-friendly messages |
| Staging directory mandatory | Prevents partial installs; atomic operations |
| Mock provider for M2 | Allows testing without native code; swappable |
| ContentResolver for file import | Respects Android permissions model |
| Room for registry | Proven Android solution; unnecessary for V1 |

## Important Decision Log

- Android-first native app ✓
- Local-first inference ✓
- llama.cpp + GGUF is runtime (not bundled) ✓
- Provider abstraction mandatory ✓
- No backend required for local mode ✓
- Storage is app-private ✓
- Models are runtime data (not source) ✓

## Next Immediate Steps

1. **Run real-native instrumentation**
   - Connect an Android device/emulator
   - Supply a compatible external GGUF using `raven.gguf.path`
   - Verify load, generation, streaming, EOS, cancellation, unload, and reload

2. **Implement basic UI** (minimal)
   - Models list screen
   - Import action
   - Model selection
   - Status display

3. **Validate build**
   - `./gradlew assembleDebug`
   - All tests passing
   - No obvious scope drift

