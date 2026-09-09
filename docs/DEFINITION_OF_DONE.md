# Definition of Done — Raven V1

V1 is complete only when all conditions below are true.

## Functional gate

1. A clean Android install launches successfully.
2. User can import a supported GGUF file via Android's system document picker.
3. User can load the imported model.
4. User can send a message and receive a streamed local response.
5. User can cancel a generation.
6. User can leave and reopen the conversation and history remains.
7. User can switch to an online provider and continue the same conversation.
8. Online errors do not corrupt conversation state.
9. Local and remote generation both pass through the same conversation orchestration layer.
10. User can create/view/edit/delete memories.
11. Memory retrieval demonstrably affects a later response.
12. User can delete all Raven data.
13. API keys never appear in normal logs.
14. App survives rotation/process recreation without losing persisted messages.

## Quality gate

- unit tests for domain/provider selection/memory rules;
- instrumentation test for core chat flow;
- integration test with a deterministic mock LLM provider;
- no known crash on the five manual acceptance scenarios in `checklists/V1_ACCEPTANCE.md`;
- UI has loading, empty, error, offline, model-loading and provider-switch states;
- README contains reproducible setup instructions;
- no unfinished placeholder screen remains in the shipped path.

## Release gate

- release APK/AAB builds locally;
- version and changelog exist;
- privacy behavior is documented;
- model licensing is not bundled blindly: model license/source must be displayed when applicable;
- large model binaries are not committed to Git.
