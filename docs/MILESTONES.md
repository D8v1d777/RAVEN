# Raven Milestones

The project is built in vertical slices. Finish each milestone and test it before starting the next.

## M0 — Foundation

Deliver:
- native Android project;
- Compose shell;
- navigation;
- dependency injection or a small manual composition root;
- Room persistence;
- basic settings persistence;
- test harness;
- CI build.

Exit criteria: clean clone builds and launches.

## M1 — Real chat without AI dependency

Deliver:
- conversation list;
- chat screen;
- message persistence;
- streaming fake provider;
- cancel/retry behavior;
- state restoration.

Exit criteria: the app feels like a real chat application using a deterministic mock provider.

## M2 — Offline model runtime

Deliver:
- llama.cpp Android integration;
- model import;
- model registry;
- load/unload lifecycle;
- streaming local generation.

Exit criteria: at least one supported GGUF model can generate a response completely offline.

## M3 — Memory

Deliver:
- explicit memory records;
- retrieval;
- memory extraction;
- memory management UI;
- deletion/export.

Exit criteria: a fact survives reopening and is later retrieved into generation context.

## M4 — Online provider

Deliver:
- OpenAI-compatible adapter;
- secure key storage;
- endpoint/model configuration;
- streaming HTTP responses;
- provider switch UI;
- error handling.

Exit criteria: the same conversation can alternate between local and online providers.

## M5 — Raven personality

Deliver:
- persona configuration;
- system prompt construction;
- tone controls;
- consistency tests;
- no business logic hard-coded to Raven's personality.

Exit criteria: personality remains consistent across local and online providers.

## M6 — Polish + release

Deliver:
- premium gothic visual system;
- animations;
- accessibility;
- performance tuning;
- crash/error states;
- release build;
- docs and demo video.

Exit criteria: every V1 requirement and definition-of-done item passes.
