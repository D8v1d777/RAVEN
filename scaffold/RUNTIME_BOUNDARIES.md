# Runtime Boundaries

Claude should preserve these boundaries while implementing the project.

```text
UI
  -> ChatViewModel
  -> ConversationUseCase
  -> LlmProviderRouter
       -> LlamaCppProvider
       -> OpenAiCompatibleProvider
```

Memory is accessed through:

```text
ConversationUseCase
  -> MemoryService
  -> MemoryRepository
```

Neither UI nor domain code should import llama.cpp native classes or HTTP client implementation details.
