# V1 Acceptance Checklist

## A. First launch

- [ ] Fresh install opens.
- [ ] User reaches chat without account creation.
- [ ] No network permission prompt is shown solely for local mode.

## B. Local model

- [ ] User opens Models.
- [ ] User selects a GGUF file with Android file picker.
- [ ] Metadata is displayed when available.
- [ ] Model imports without blocking UI indefinitely.
- [ ] User loads model.
- [ ] Model status shows ready.
- [ ] Airplane mode enabled.
- [ ] User sends message.
- [ ] Raven streams response.
- [ ] User cancels generation successfully.

## C. Memory

- [ ] User creates/approves a memory.
- [ ] Memory appears in memory viewer.
- [ ] Conversation is closed and reopened.
- [ ] Memory remains.
- [ ] A relevant future prompt retrieves the memory.
- [ ] User deletes the memory.
- [ ] It no longer appears in retrieval.

## D. Online

- [ ] User explicitly selects Online.
- [ ] Provider settings are configurable.
- [ ] API key is stored securely.
- [ ] Online response streams.
- [ ] Switching back to Local works.
- [ ] Conversation history remains intact.
- [ ] Network failure produces an understandable error.

## E. Data control

- [ ] Export works.
- [ ] Delete all data works.
- [ ] API secrets are not present in logs.

## F. Release

- [ ] Release APK/AAB builds.
- [ ] No placeholder screens remain.
- [ ] No debug-only secrets.
- [ ] README setup works from a clean clone.
