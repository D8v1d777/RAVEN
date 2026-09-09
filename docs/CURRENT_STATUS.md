# Raven Current Status

Last updated: 2026-09-04 (M2.3 runtime validation harness)

## Highest verified runtime level

**LEVEL 3 — native library packaged in the debug APK**

Verified:

- CMake configuration and llama.cpp compilation pass for `arm64-v8a` and `x86_64`.
- `raven_llama.cpp` compiles and links into `libraven_llama.so`.
- APK contains `lib/arm64-v8a/libraven_llama.so` and `lib/x86_64/libraven_llama.so`.
- JVM tests pass: 28 tests.
- Lint passes.

Not verified:

- Native library loading on an Android device.
- Real GGUF loading or generation.
- Streaming, EOS, cancellation, unload/reload, or performance.

## M2.3 validation harness

`RealNativeInferenceSmokeTest` is an Android instrumentation test that accepts an externally supplied, device-readable GGUF path through the `raven.gguf.path` instrumentation argument. It imports the file through `ImportModelUseCase`, registers it in an in-memory Room repository, selects it through `LocalLlmProvider`, performs real streaming, unloads, reloads, and performs a second generation.

The harness compiles successfully but has not executed because the current environment has no connected Android device/emulator and no external GGUF file.

Required external setup:

1. Connect an Android device or start an emulator with a supported ABI.
2. Push a compatible GGUF outside the repository, for example to `/data/local/tmp/model.gguf`.
3. Build/install the debug and test APKs.
4. Run `RealNativeInferenceSmokeTest` with `-e raven.gguf.path /data/local/tmp/model.gguf`.

No model weights are stored in Git, assets, resources, or the APK source tree.
