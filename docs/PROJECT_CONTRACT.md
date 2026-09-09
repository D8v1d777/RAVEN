# Raven Project Contract

Status: FROZEN FOR V1
Owner: Indie developer
Primary platform: Android
Primary deployment mode: local/offline
Budget target: $0 recurring infrastructure cost

## Core objective

Build a polished Android companion app named Raven that can run a supported local GGUF LLM on-device and can also use a remote model through a pluggable API provider. Conversation history and user-approved memories must survive provider/model changes.

## Primary user story

A user installs Raven, imports a compatible GGUF model, selects Offline mode, opens a conversation, and chats with Raven with no network connection. The user can later select Online mode, configure a compatible provider endpoint/API key, and continue the same conversation without losing memory.

## V1 requirements

### Conversation
- streaming token UI;
- send/cancel/retry;
- conversation history;
- regenerate assistant response;
- copy/share message;
- empty/loading/error states;
- provider/model indicator;
- offline/online status indicator.

### Local inference
- local GGUF model import;
- model metadata display;
- model selection;
- model load/unload status;
- generation settings: temperature, top-p, max output tokens, context size where supported;
- graceful out-of-memory/error handling;
- no mandatory network connection.

### Online inference
- provider abstraction, not provider-specific code in UI;
- at least one OpenAI-compatible provider adapter;
- streaming responses;
- API key stored securely on-device;
- endpoint/model editable by the user;
- clear online/offline indicator;
- network error recovery.

### Memory
- conversation messages stored locally;
- explicit memory records;
- memory retrieval before generation;
- memory extraction after generation;
- memory viewer;
- edit/delete memory;
- global "forget everything" action;
- export/delete all app data.

### Privacy
- no account required for core local mode;
- no analytics SDK in v1;
- no telemetry by default;
- no secret/API key logging;
- online provider calls happen only when Online mode is selected;
- local data remains on device unless user explicitly exports or sends it to a remote provider through Online mode.

### Model portability

The model/runtime layer must not be coupled to Raven's memory or UI. A model provider can be swapped without changing conversation/domain code.

## Hard constraints

- Android first; desktop is not a target for v1.
- $0 budget is the design assumption.
- Prefer local/open-source dependencies.
- Avoid paid infrastructure.
- Avoid unnecessary backend services.
- Do not add a dependency merely because it makes a demo faster if the feature can be implemented with existing Android APIs cleanly.
- Do not redesign finished modules without a concrete failing requirement.

## Explicitly deferred

Put ideas into `docs/FUTURE.md` instead of implementing them during v1:

- voice conversation
- speech-to-text
- proactive notifications
- background companion service
- avatar/animation
- image generation
- web search/tools
- long-term cloud backup
- embeddings/vector database
- relationship/game systems
- multi-agent architecture
