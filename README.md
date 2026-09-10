# 🖤 RAVEN

### *Android-First Local / Online AI Companion*

<!-- ═══════════════════════ RAVEN AVATAR ═══════════════════════ -->

<!--
     DROP YOUR RAVEN PROFILE IMAGE HERE
     Replace the placeholder below with:
     ![Raven](path/to/raven-avatar.png)
-->

<p align="center">
  <img src="YOUR_RAVEN_IMAGE_HERE.png" alt="Raven — Gothic AI Companion" width="320"/>
</p>

<p align="center">
  <img src="docs/branding/raven-profile/avatar-frame.svg" alt="Raven Avatar Frame" width="360"/>
</p>

<p align="center">

### `THE SHADOW BETWEEN THOUGHT AND WORD`

*An offline-first AI companion built to live on your device.*

</p>

<p align="center">
  <img src="docs/branding/raven-profile/gothic-divider.svg" alt="divider"/>
</p>

> **Raven** is an Android-first AI companion focused on local intelligence, provider independence, persistent memory, and a deliberately finishable v1.

<p align="center">
  🖤 Local-first &nbsp; • &nbsp; 🩸 Private by design &nbsp; • &nbsp; 🪶 GGUF &nbsp; • &nbsp; 🌙 Android-first
</p>

---

## 🕯️ About Raven

Raven is designed around four principles:

|     | Principle                 | Meaning                                                                                |
| --- | ------------------------- | -------------------------------------------------------------------------------------- |
| 🖤  | **Local-first inference** | Run AI locally without requiring a server or paid AI API.                              |
| 🩸  | **Provider independence** | Switch between local and remote model providers through the same conversation runtime. |
| 🪶  | **Persistent memory**     | Preserve useful memories without depending on an enormous context window.              |
| 🕯️ | **Finishability**         | Keep v1 intentionally small with an explicit definition of done.                       |

<p align="center">
  <img src="docs/branding/raven-profile/raven-feather-divider.svg" alt="feather divider"/>
</p>

## 🦇 v1 Product Promise

A user can:

* Chat with Raven locally using a GGUF model through **llama.cpp**.
* Switch to an online provider through an API adapter.
* Preserve conversation history and selected memories across model switches.
* See which provider and model are currently active.
* Import and remove local GGUF models through the Android document picker.
* Export or delete Raven data.
* Use Raven without an account.

---

## 🌑 What Raven Is Not

The first version deliberately avoids unnecessary scope.

* 3D avatars
* Autonomous web browsing
* Autonomous background messaging
* Social or multi-user accounts
* Cloud-synchronized memory
* Model fine-tuning inside the app
* Image generation
* Multi-agent swarms
* Smart-home / device control
* Monetization and billing

> **The goal is not to build everything.**
>
> **The goal is to build Raven properly.**

---

## 🥀 The Character

Raven is a fictional gothic-styled AI companion.

Her personality direction is:

**Warm. Witty. Observant. Slightly dark. Conversationally consistent.**

The gothic aesthetic is part of the character identity, while the actual persona remains configurable rather than hard-coded into business logic.

<p align="center">
  <img src="docs/branding/raven-profile/quote-panel.svg" alt="Raven quote panel"/>
</p>

> *“Somewhere between silence and thought, Raven listens.”*

---

## 🩶 Runtime Architecture

<p align="center">
  <img src="docs/branding/raven-profile/architecture-diagram.svg" alt="Raven architecture"/>
</p>

```text
┌───────────────────────────────┐
│            RAVEN UI           │
│       Android / Compose       │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│            DOMAIN             │
│ Generation / Conversation     │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│        PROVIDER ROUTER        │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│       LOCAL LLM PROVIDER      │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│    LOCAL INFERENCE RUNTIME    │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│             JNI               │
│           C / C++             │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│          llama.cpp            │
└───────────────┬───────────────┘
                │
                ▼
┌───────────────────────────────┐
│          GGUF MODEL           │
└───────────────────────────────┘
```

---

## 🕸️ Technology

<p align="center">

`Android` · `Kotlin` · `Jetpack Compose` · `C/C++` · `JNI` · `llama.cpp` · `GGUF` · `Gradle`

</p>

| Layer          | Technology                         |
| -------------- | ---------------------------------- |
| Platform       | Android                            |
| UI             | Jetpack Compose                    |
| Language       | Kotlin                             |
| Native runtime | C / C++                            |
| Model runtime  | llama.cpp                          |
| Model format   | GGUF                               |
| Native bridge  | JNI                                |
| Build          | Gradle / Android Gradle Plugin     |
| Storage        | Android app-private storage + Room |

---

## 🪶 Core Features

### 🖤 Offline Intelligence

Run local GGUF models directly on Android without depending on a remote AI API.

### 🩸 Provider Independence

The same conversation architecture can support local and remote inference providers.

### 🕯️ Persistent Memory

Conversation history and selected memories can persist across model/provider changes.

### 🪶 Local Model Storage

Models are kept inside Raven's app-accessible storage and tracked through its model registry.

### 🌙 Streaming Generation

The runtime architecture supports token-by-token generation events rather than waiting for the entire response.

---

## 🗝️ Current Development Status

| Component                       | Status |
| ------------------------------- | ------ |
| Android project                 | ✅      |
| Gradle build                    | ✅      |
| Native compilation              | ✅      |
| llama.cpp integration           | ✅      |
| GGUF infrastructure             | ✅      |
| Local model storage             | ✅      |
| Model import pipeline           | ✅      |
| Gemma compatibility             | ✅      |
| Real Gemma GGUF acquired        | ✅      |
| Physical Android device test    | ⏳      |
| Real model loading on device    | ⏳      |
| Real token generation on device | ⏳      |

> **Important:** build and source-level compatibility do not yet mean that real Gemma generation has been verified on a physical Android device.

---

## 🩸 First Model

Raven's first real model target is:

```text
Model:          Google Gemma 2B Instruct
Architecture:   Gemma 1
Format:         GGUF
Quantization:   Q4_K_M
File:           gemma-2b-it-q4_k_m.gguf
Approx. size:   ~1.5 GB
```

The model is intended as the first practical smoke-test target for Raven's local inference pipeline.

---

## 🕯️ Roadmap

```text
[✓] Android foundation
[✓] Local model infrastructure
[✓] llama.cpp native integration
[✓] GGUF model pipeline
[✓] Windows debug APK build
[✓] Gemma GGUF acquisition
[ ] Real import verification
[ ] Physical Android deployment
[ ] Native Gemma load
[ ] Real prompt generation
[ ] Streaming generation verification
[ ] v1 polish
```

---

## 🦇 Project Philosophy

Raven isn't meant to be another cloud wrapper.

She's meant to feel like something that **belongs to the device**.

No mandatory account.
No mandatory server.
No dependency on one provider.

Just a local model, a conversation, memory, and a little darkness.

<p align="center">
  <img src="docs/branding/raven-profile/section-header.svg" alt="Raven section header"/>
</p>

## 🖤 Project Information

**Project:** Raven
**Platform:** Android
**Primary focus:** Local / offline AI
**Model format:** GGUF
**Native runtime:** llama.cpp
**Character:** Gothic AI companion

---

## 📜 Source of Truth

Before modifying the project, read these documents in order:

1. `docs/PROJECT_CONTRACT.md`
2. `docs/DEFINITION_OF_DONE.md`
3. `docs/ARCHITECTURE.md`
4. `docs/MILESTONES.md`
5. `claude/CLAUDE.md`
6. `claude/TASK_PROTOCOL.md`
7. `contracts/*.md`

Never invent requirements when these documents already define them.

---

<p align="center">
  <img src="docs/branding/raven-profile/gothic-divider.svg" alt="divider"/>
</p>

<p align="center">

### 🪶 RAVEN

*The shadow between thought and word.*

**Local by nature.
Independent by design.
A little darker than the rest.**

</p>

<p align="center">
  <img src="docs/branding/raven-profile/raven-feather-divider.svg" alt="feather divider"/>
</p>

<p align="center">

`RAVEN` · `OFFLINE AI` · `ANDROID` · `GGUF` · `LLAMA.CPP`

</p>
