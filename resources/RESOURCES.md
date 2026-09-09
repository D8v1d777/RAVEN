# Raven Engineering Resources

These are the primary resources Claude should consult rather than relying on random tutorials.

## Android UI / architecture

- Jetpack Compose architecture: https://developer.android.com/develop/ui/compose/architecture
- Compose state: https://developer.android.com/develop/ui/compose/state
- Android Jetpack: https://developer.android.com/jetpack

## Local inference

- llama.cpp Android documentation: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- llama.cpp Android example: https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android
- llama.cpp build documentation: https://github.com/ggml-org/llama.cpp/blob/master/docs/build.md

## Android model/file handling

- Storage Access Framework: https://developer.android.com/training/data-storage/shared/documents-files

## Background execution

- Foreground services overview: https://developer.android.com/develop/background-work/services/fgs
- Foreground service restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start

## Online provider abstraction

Implement a generic HTTP provider interface first. Provider-specific official SDKs can be added later behind the same interface. Do not let an SDK type leak into domain code.

## Model source policy

Do not bundle a model into Git. Document the exact model identifier, quantization, download source and license in `docs/MODEL_POLICY.md` for each tested model.
