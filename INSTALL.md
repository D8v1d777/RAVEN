# 🥀 Raven Installation Guide

*From an empty machine to talking to Raven — in the correct order.*

> **Read this top to bottom.** Every stage depends on the one before it.
> Estimated total time: ~45–60 minutes (most of it waiting for downloads).

---

## 🕯️ Stage 0 — Prerequisites

Install these **before** touching the repo.

| Requirement | Version | Why |
| ----------- | ------- | --- |
| Git | any recent | Clone the repository |
| JDK | **17** (exactly) | `app/build.gradle` targets Java/JVM 17 — JDK 21 will fail |
| Android Studio | Ladybug (2024.2) or newer | Easiest way to get the SDK, NDK and CMake |
| Disk space | ~10 GB free | SDK + NDK + CMake + Gradle caches + model |
| Android device | arm64-v8a, Android 5.0+ (API 21+) | Physical device recommended |
| — or emulator | x86_64 image, API 34 | Both ABIs are built: `arm64-v8a` and `x86_64` |

**Verify your JDK on Windows:**

```powershell
java -version        # must print: 17.x.x
```

If it prints anything else, install JDK 17 (e.g. Temurin from
<https://adoptium.net>) and set `JAVA_HOME` to its folder.

---

## 🌑 Stage 1 — Download Raven

```powershell
git clone https://github.com/D8v1d777/RAVEN.git
cd RAVEN
```

Nothing else needs to be fetched — **llama.cpp is already vendored** under
`third_party/llama.cpp`, so the native build works straight from the clone.

---

## 🩸 Stage 2 — Install the Android toolchain

Open **Android Studio → More Actions → SDK Manager → SDK Tools tab** and
check *Show Package Details*, then install these exact components:

| Component | Exact version | Notes |
| --------- | ------------- | ----- |
| Android SDK Platform | **34** | `compileSdk` / `targetSdk` in `app/build.gradle` |
| NDK (Side by side) | **29.0.13113456** | Pinned via `ndkVersion` — other versions may fail |
| CMake | **3.31.6** | Pinned via `externalNativeBuild.cmake.version` |
| Android SDK Build-Tools | 34.0.0 | Standard companion for API 34 |
| SDK Command-line Tools | latest | Provides `sdkmanager` / `adb` |

**Option A — Android Studio:**
1. *File → Open* → select the cloned `RAVEN` folder.
2. Wait for *Gradle Sync* to finish (first sync downloads all dependencies).
3. *Build → Make Project*.

**Option B — command line:**

```powershell
.\gradlew.bat assembleDebug
```

First build compiles llama.cpp natively — **expect 10–25 minutes**. Later
builds are much faster.

The output APK lands at:
`app/build/outputs/apk/debug/app-debug.apk`

**Optionally run the JVM test suite:**

```powershell
.\gradlew.bat test
```

---

## 🌙 Stage 4 — Install on a device

1. Enable **Developer options** on the phone (tap *Build number* 7×), then
   enable **USB debugging**.
2. Connect the device and confirm the debugging prompt.
3. Either press **Run ▶** in Android Studio, or:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

4. Launch **Raven** on the device. It starts with no model loaded — that is
   normal.

> Prefer a **physical arm64 device** for inference speed. An x86_64 emulator
> works, but local generation is noticeably slower.

---

## 🪶 Stage 5 — Get the model (Raven's brain)

Raven ships **no model**. You bring your own GGUF. The first target model is:

```text
Model:          Google Gemma 2B Instruct
Architecture:   Gemma 1
Format:         GGUF
Quantization:   Q4_K_M
File:           gemma-2b-it-q4_k_m.gguf
Approx. size:   ~1.5 GB
```

**Where to get it:** download `gemma-2b-it-q4_k_m.gguf` from Hugging Face
(search *"gemma-2b-it Q4_K_M gguf"* — e.g. the `bartowski/gemma-2b-it-GGUF`
mirror). Accept Gemma's license where prompted.

> Any small Gemma-architecture GGUF works; the file is ~1.5 GB, so make sure
> the device has room. Models are gitignored — **never** commit one.

**Get it onto the device (pick one):**

* Download directly in the phone's browser, **or**
* Download on the PC and push it:

```powershell
adb push gemma-2b-it-q4_k_m.gguf /sdcard/Download/
```

---

## 🖤 Stage 6 — Import the model & talk to Raven

1. Open Raven → open the **model sheet** (the model picker in the app).
2. Choose **Import model** → the Android document picker opens.
3. Navigate to the GGUF file (e.g. `Download/gemma-2b-it-q4_k_m.gguf`).
   Raven validates the GGUF header and copies it into its private storage.
4. Select the imported model. The active **provider/model indicator** should
   now show the local Gemma model.
5. Type. Raven answers **locally, offline, through llama.cpp** — first token
   takes a while (model load), subsequent tokens stream in.

> First response on a physical device is the real smoke test. If generation
> seems stuck, make sure you installed a **debug** (or optimized) build — the
> native layer is always built with `-DCMAKE_BUILD_TYPE=Release`, but a very
> old cached `.cxx` build directory can be cleared with
> `.\gradlew.bat clean` before rebuilding.

---

## ⚙️ Stage 7 — Optional verification

* Run the native smoke test on the connected device:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

* Run the JVM unit tests:

```powershell
.\gradlew.bat test
```

---

## 🩸 Troubleshooting

| Symptom | Fix |
| ------- | --- |
| `Unsupported class file major version` / Gradle fails at once | Wrong JDK — install **JDK 17** and set `JAVA_HOME` |
| `NDK not configured` / version mismatch | Install NDK **29.0.13113456** via SDK Manager |
| `CMake '3.31.6' was not found` | Install CMake via SDK Manager, or relax the pinned `cmake.version` |
| `SDK location not found` | Create `local.properties` with `sdk.dir=...` (Stage 2) |
| First sync is very slow | Normal — Gradle downloads the 8.5 distribution and all dependencies |
| Import fails with "not a valid GGUF" | The file is corrupted/incomplete — re-download and check the size (~1.5 GB) |
| Generation is extremely slow | Use a physical arm64 device, not an emulator; don't strip the Release flags |

---

## ✅ Correct order — summary

```text
[0] Install Git + JDK 17 + Android Studio
[1] git clone https://github.com/D8v1d777/RAVEN.git
[2] SDK Manager: Platform 34 + NDK 29.0.13113456 + CMake 3.31.6 (+ local.properties)
[3] .\gradlew.bat assembleDebug   (or Android Studio Make Project)
[4] adb install app-debug.apk     (or Run ▶)
[5] Download gemma-2b-it-q4_k_m.gguf (~1.5 GB) → put it on the device
[6] In-app: import the GGUF via the document picker → select it → chat
[7] Optional: connectedDebugAndroidTest / test
```

---

> *Darkness installed. The rest is conversation.*

`RAVEN` · `INSTALL` · `GGUF` · `LLAMA.CPP` · `ANDROID`


If you prefer the command line instead of Android Studio:

```powershell
sdkmanager "platforms;android-34" "build-tools;34.0.0" "ndk;29.0.13113456" "cmake;3.31.6"
```

> If your SDK Manager does not list CMake 3.31.6, install the closest 3.31.x
> it offers and update `externalNativeBuild.cmake.version` in
> `app/build.gradle` to match.

**Point Gradle at your SDK** — create `local.properties` in the repo root
(Android Studio does this automatically):

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

> `local.properties` is gitignored on purpose — never commit it.

---

## 🕯️ Stage 3 — Build

The Gradle wrapper (8.5) downloads itself on first run — no global Gradle
install needed.
