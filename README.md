# RAVEN — Android-First Local/Online AI Companion

Raven is an Android-first AI companion focused on four principles:

1. Local-first inference: the app can work without a server or paid AI API.
2. Provider independence: the same conversation runtime can switch between local and remote models.
3. Persistent, inspectable memory: Raven remembers useful information without requiring a giant context window.
4. Finishability: v1 is deliberately small and has an explicit definition of done.

## v1 product promise

A user can:
- chat with Raven locally using a GGUF model through llama.cpp;
- switch to an online provider through an API adapter;
- preserve conversation history and selected memories across model switches;
- see which provider/model is active;
- import/remove local GGUF models through the Android document picker;
- export/delete their Raven data;
- Raven data;
- use the app without an account.

## Non-goals for v1

- 3D avatars
- autonomous web browsing
- autonomous background messaging
- social/multi-user accounts
- cloud-synchronized memory
- model fine-tuning inside the app
- image generation
- multi-agent swarms
- smart-home/device control
- monetization/billing

## Product name

Raven

## Character direction

Raven is a fictional gothic-styled companion. The persona is warm, witty, observant, slightly dark in aesthetic, and conversationally consistent. Persona data is configuration, not hard-coded business logic.

## Source of truth

Read these in order before changing the project:

1. `docs/PROJECT_CONTRACT.md`
2. `docs/DEFINITION_OF_DONE.md`
3. `docs/ARCHITECTURE.md`
4. `docs/MILESTONES.md`
5. `claude/CLAUDE.md`
6. `claude/TASK_PROTOCOL.md`
7. `contracts/*.md`

Never invent requirements when these documents already define them.
# RAVEN