# Model Policy

Raven is a runtime, not a model distribution project.

## Requirements for a tested local model

Record:
- model family/name;
- exact repository/model identifier;
- GGUF quantization;
- file size;
- expected RAM requirement;
- context length used for testing;
- license;
- download/source URL;
- device used for benchmark.

## Device tiers

Do not assume a model works on every Android phone.

Create benchmark profiles later for:
- low-memory Android device;
- mid-range device;
- high-end device.

## Safety against OOM

Before loading a model, display estimated size/resource requirements when metadata allows. Prefer a controlled failure message over crashing the app.
