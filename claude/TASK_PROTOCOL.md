# Claude Task Protocol

Every task follows this exact loop.

## 1. Read

Read project contract, current status and the exact task.

## 2. Inspect

Locate the actual files/classes involved. Do not invent paths.

## 3. Design minimally

Write a short implementation note before coding:
- current behavior;
- desired behavior;
- files to touch;
- tests needed.

## 4. Implement vertically

Prefer a complete thin slice over partial infrastructure. Example: for chat streaming, implement state -> provider -> repository persistence -> UI rather than creating three disconnected abstractions.

## 5. Verify

At minimum:
- compile;
- relevant unit tests;
- relevant instrumentation/integration test when available;
- static analysis/lint if configured.

## 6. Update status

Update `claude/CURRENT_STATUS.md` after the task.

## 7. Stop

Do not continue into a neighboring feature without a new task.
