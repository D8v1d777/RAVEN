# Claude Operating Instructions — Raven

You are the primary implementation agent for Raven. The human developer is a mediator and may relay your output into another session. Therefore every meaningful implementation step must be explicit, reproducible and recorded in the repository.

## Before changing code

Read:
1. `docs/PROJECT_CONTRACT.md`
2. `docs/DEFINITION_OF_DONE.md`
3. `docs/ARCHITECTURE.md`
4. `docs/MILESTONES.md`
5. this file
6. `claude/TASK_PROTOCOL.md`
7. the relevant contract under `contracts/`

Then inspect the current repository instead of assuming a file exists.

## Scope discipline

- Work only on the assigned task.
- Do not add features from `docs/FUTURE.md`.
- Do not replace architecture merely because another architecture is fashionable.
- Do not introduce cloud infrastructure for convenience.
- Do not add a dependency unless it solves a concrete requirement and is compatible with the $0/local-first goal.
- Do not modify unrelated files.
- If you discover a future improvement, document it rather than implementing it.

## Implementation discipline

1. Inspect existing code.
2. State the smallest change set required.
3. Implement it.
4. Write/update tests.
5. Run the narrowest relevant tests first.
6. Run build/lint checks.
7. Update status documentation.
8. Report exactly what changed, what passed, and what remains.

## Android rules

Use modern Android architecture with Compose and unidirectional data flow. Keep UI state out of repositories. Keep platform/native inference details behind interfaces.

Use the Android Storage Access Framework for user-selected GGUF files rather than assuming filesystem paths.

Do not add background services unless a concrete requirement demands one and the Android foreground-service rules have been reviewed. Long-running/background work is constrained on modern Android versions.

## Model rules

Local inference is a first-class path, not a demo fallback.

The offline provider must not require an internet connection.

Online providers must be selected explicitly by the user. Do not silently upload conversation content to a remote model.

## Security rules

- Never log API keys.
- Never hard-code credentials.
- Never commit model binaries or secrets.
- Keep network permissions minimal.
- Do not use analytics by default.
- Preserve user deletion/export controls.

## Finishability rule

A task is finished only when its acceptance criteria pass. Do not declare success merely because the code compiles.

## Communication format

At the end of every task, report:
- Implemented
- Files changed
- Tests run + results
- Known limitations
- Next task (one task only)
