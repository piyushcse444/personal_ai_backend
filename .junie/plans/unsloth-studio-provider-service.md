---
sessionId: session-261114-095336-1s7p
---

# Requirements

### Overview
Create a `UnslothProviderApiServiceImpl` that communicates with the Unsloth Studio API (running on port `8888`) to manage model loading/unloading and respond to chat requests from the frontend using the Unsloth OpenAI-compatible API endpoints.

### Goals
1. When a request arrives for the offline Unsloth Studio model, ensure the model info exists in the `models` table with `isOffline=true` and a large context window.
2. Check Unsloth API health, verify the model exists, check if it's loaded and serving, and load it if needed.
3. Handle model loading concurrency — coalesce concurrent requests so only one load operation runs.
4. Send the chat prompt to the Unsloth API and stream the response back to the frontend via SSE (Server-Sent Events), including thinking/reasoning steps if the model produces them.
5. Support enable_thinking, enable_tools (web_search, python, terminal), and other Unsloth-specific parameters.

### User Stories
- As a user, when I select the offline model and send a message, I want to see tokens appear in real-time as the model generates them.
- As a user, I want to see the model's thinking/reasoning steps if it produces them, so I can follow its logic.
- As a user, I want the model to support tools like web search, Python execution, and terminal access.
- As a developer, I want concurrent requests during model loading to wait rather than trigger multiple load operations.

### Scope
**In Scope:**
- `UnslothProviderApiServiceImpl` — provider implementation using Unsloth API
- `UnslothServerManager` — manages model lifecycle via Unsloth API (health check, list models, load/unload, inference status)
- SSE streaming endpoint in `ConversationController` for the offline model
- Model seeded in the database via Flyway migration on startup
- Configuration properties in `application.yml`
- Concurrency control for model loading

**Out of Scope:**
- Managing the Unsloth Studio process itself (assumes Unsloth is already running)
- Model download management (assumes model is available in Unsloth)
- Starting/stopping the Unsloth Studio application

# Technical Design

### Current Implementation
- **ProviderApiService** interface (`service/ProviderApiService.java`) defines `chat(List<Message>, Model, ProviderConfig, boolean thinking)` returning `ProviderApiResponse`.
- **ProviderApiServiceFactory** (`service/ProviderApiServiceFactory.java`) maps provider codes (`openai`, `anthropic`, `google`, `mistral`, `ollama`, `unsloth`) to implementations.
- **ConversationController.sendMessage()** calls the provider API synchronously and returns the full response.
- SSE streaming via `POST /api/conversations/{id}/messages/stream` returns `SseEmitter`.

### Key Decisions
1. **Unsloth Studio API integration** — Instead of managing a local `llama-cli` process, the backend communicates with the Unsloth Studio API running on port 8888. This provides model management, health checks, and OpenAI-compatible chat completions.
2. **Health vs model readiness separation** — `/api/health` returning healthy only means the Unsloth service is up, not that the model is loaded. `ensureModelReady()` follows a strict flow: health → model exists → model loaded → model serving.
3. **Concurrency control for model loading** — Uses `ConcurrentHashMap` with `CompletableFuture` to coalesce concurrent requests during model loading. Only one thread triggers the load; others wait on the same future.
4. **SSE with thinking visibility** — Stream tokens via SSE; parse and expose reasoning/thinking steps separately via `enable_thinking` parameter.
5. **Tool support** — The Unsloth API supports `enable_tools` with `enabled_tools` array (web_search, python, terminal).
6. **New provider code `unsloth`** — Registered in `ProviderApiServiceFactory`.
7. **Model seeded via Flyway migration** — The Unsloth Studio provider and model are inserted into the database via `V5__add_unsloth_provider_and_model.sql` with `isOffline=true` and `contextWindow=8192`.

### Architecture Diagram

``` mermaid
graph LR
    A[Frontend] -->|POST /api/conversations/{id}/messages| B[ConversationController]
    B -->|provider=unsloth| C[UnslothProviderApiServiceImpl]
    C -->|ensureModelReady| D[UnslothServerManager]
    D -->|/api/health| E[Unsloth API :8888]
    D -->|/v1/models| E
    D -->|/api/inference/status| E
    D -->|/api/inference/load| E
    D -->|/api/inference/unload| E
    C -->|POST /v1/chat/completions stream=true| E
    E -->|SSE token chunks| C
    C -->|SseEmitter chunks| B
    B -->|text/event-stream| A
```

### Proposed Changes

#### 1. UnslothServerManager (refactored service)
- Manages model lifecycle via Unsloth Studio API.
- `checkHealth()` — calls `/api/health` to verify Unsloth service is healthy.
- `listModels()` — calls `/v1/models` to list available models.
- `getInferenceStatus()` — calls `/api/inference/status` to get loaded/serving models and engine info.
- `loadModel()` — calls `/api/inference/load` to load a model with max_seq_length.
- `unloadModel()` — calls `/api/inference/unload` to unload a model.
- `isModelLoaded()` — checks if a model is in the loaded list.
- `ensureModelReady()` — orchestrates the full flow: health → model exists → loaded → serving → load if needed.
- Concurrency control via `ConcurrentHashMap<String, CompletableFuture<Boolean>>` to coalesce concurrent load requests.
- Polling with timeout (up to 30 seconds, 1-second intervals) after load to verify model is serving.

#### 2. UnslothProviderApiServiceImpl (refactored service)
- Implements `ProviderApiService`.
- `chat()` method: ensures model is ready via `ensureModelReady()`, then calls `/v1/chat/completions` (non-streaming).
- Returns `ProviderApiResponse` for the synchronous path.
- `chatStreaming()` method: returns `SseEmitter` that streams tokens from `/v1/chat/completions` with `stream: true`.
- Builds Unsloth-specific request body with `enable_thinking`, `enable_tools`, `enabled_tools` (web_search, python, terminal), `permission_mode`, and generation params.
- Parses SSE chunks, separating thinking/reasoning content from final answer.
- Saves only the final response (non-thinking) to the database after streaming completes.

#### 3. Streaming endpoint in ConversationController
- Endpoint `POST /api/conversations/{id}/messages/stream` that returns `SseEmitter`.
- For `unsloth` provider, uses `chatStreaming()` with thinking and tools support.
- Emits token chunks as they arrive; thinking content marked with `isThinking` flag.

#### 4. Model Seeded via Flyway Migration
- `V5__add_unsloth_provider_and_model.sql` inserts the `unsloth` provider (`code=unsloth`, `type=LOCAL`) and the model (`isOffline=true`, `contextWindow=8192`) into the database.
- No runtime Java code is needed — the model is available in the DB before the app starts serving requests.

#### 5. Configuration Properties
- `app.unsloth.enabled=true`
- `app.unsloth.base-url=http://127.0.0.1:8888`
- `app.unsloth.api-key=sk-unsloth-...` (Bearer token for API auth)
- `app.unsloth.max-seq-length=100000`
- **No model list in config** — Models are discovered dynamically from Unsloth's `/v1/models` endpoint. Adding a new model only requires a row in the `models` database table pointing to the `unsloth` provider. The quantization variant is resolved at runtime from the API response.

### Data Models / Contracts

**SSE Event Format (streaming endpoint):**
```
event: token
data: {"content": "Hello", "isThinking": false}

event: token
data: {"content": "Let me think...", "isThinking": true}

event: done
data: {"inputTokens": 42, "outputTokens": 128}

event: error
data: {"message": "Server not responding"}
```

**UnslothServerManager interface:**
```java
public interface UnslothServerManager {
    boolean checkHealth();
    List<ModelInfo> listModels();
    InferenceStatus getInferenceStatus();
    boolean loadModel(String modelPath, int maxSeqLength);
    boolean unloadModel(String modelPath);
    boolean isModelLoaded(String modelPath);
    boolean ensureModelReady(String modelPath);

    record ModelInfo(String id, String object, long created, String ownedBy,
                     boolean loaded, String quant, String displayName, String statusValue) {}

    record InferenceStatus(String activeModel, String modelIdentifier,
                           List<String> loaded, List<String> serving,
                           int contextLength, int maxContextLength,
                           boolean supportsReasoning, boolean supportsTools,
                           String ggufVariant) {}
}
```

**Unsloth Chat Request Body:**
```json
{
  "model": "AtomicChat/LFM3-1.7B-Uncensored-GGUF:Q4_K_M",
  "messages": [{"role": "user", "content": "What is Unsloth?"}],
  "temperature": 0.7,
  "top_p": 0.8,
  "top_k": 20,
  "min_p": 0.05,
  "repetition_penalty": 1.1,
  "max_tokens": 1024,
  "enable_thinking": true,
  "enable_tools": true,
  "enabled_tools": ["web_search", "python", "terminal"],
  "permission_mode": "off",
  "stream": true
}
```

**Vision Model Chat Request Body (with image):**
```json
{
  "model": "HauhauCS/Gemma-4-E4B-Uncensored-HauhauCS-Aggressive:Q2_K_P",
  "messages": [
    {
      "role": "user",
      "content": [
        {"type": "text", "text": "Describe this image."},
        {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,<BASE64_DATA>"}}
      ]
    }
  ],
  "temperature": 0.7,
  "top_p": 0.8,
  "top_k": 20,
  "min_p": 0.05,
  "repetition_penalty": 1.1,
  "max_tokens": 1024,
  "enable_thinking": true,
  "enable_tools": true,
  "enabled_tools": ["web_search", "python", "terminal"],
  "permission_mode": "off",
  "stream": true
}
```

The `supports_vision` column in the `models` table indicates which models can process images (e.g., Gemma-4-E4B supports vision, LFM3-1.7B does not). The frontend uses this flag to enable/disable the image upload button. When an image is sent, `SendMessageRequest` includes `imageBase64` and `imageMimeType` fields, and the backend formats the message using the multi-content array format (`type: text` + `type: image_url`).

The `model` field is dynamically resolved from the conversation's `Model` entity. The quant suffix (e.g., `:Q4_K_M` or `:Q2_K_P`) is resolved at runtime from the cached `/v1/models` response — no config needed. The cache is populated during `ensureModelReady()` and provides instant lookups via `getQuantForModel()`.

### File Structure
```
src/main/java/com/personal_ai/backend/
├── resources/db/migration/
│   └── V5__add_unsloth_provider_and_model.sql (seeds provider and model)
├── service/model/
│   ├── UnslothServerManager.java             (interface — Unsloth API management)
│   └── impl/
│       ├── UnslothServerManagerImpl.java     (Unsloth API client)
│       └── UnslothProviderApiServiceImpl.java (provider API — chat and streaming)
├── controller/
│   └── ConversationController.java            (streaming endpoint)
src/main/resources/
└── application.yml                            (unsloth config — base-url, api-key, model)
```

### Risks
- **Unsloth Studio availability**: The backend assumes Unsloth Studio is running on the configured port. If it's down, health checks will fail and model requests will be rejected with clear error messages.
- **Model loading time**: Loading a model via Unsloth API can take 10-30 seconds. Concurrency control ensures only one load operation runs, with polling (up to 30 seconds) to verify the model is serving.
- **SSE connection lifecycle**: If the client disconnects, the `SseEmitter.complete()` must be called to avoid resource leaks.
- **Thinking parsing**: The model may not produce structured thinking markers. The Unsloth API returns `reasoning_content` in SSE chunks, which is parsed and streamed separately.
- **API key security**: The Unsloth API key is stored in `application.yml`. In production, use environment variables or a secrets manager.

# Testing

### Validation Approach
Since the Unsloth Studio API requires the service to be running locally, full integration testing depends on the local environment. The plan focuses on unit-level validation and manual smoke tests.

### Key Scenarios
1. **Health check**: `checkHealth()` returns true when Unsloth API is running and healthy.
2. **Model listing**: `listModels()` returns the available models from `/v1/models`.
3. **Model loading**: `loadModel()` successfully loads a model via `/api/inference/load`.
4. **Concurrency control**: Concurrent requests during model loading coalesce to a single load operation.
5. **Ensure model ready**: Full flow — health → model exists → loaded → serving — works correctly.
6. **Chat response**: `UnslothProviderApiServiceImpl.chat()` returns a `ProviderApiResponse` with content from the model.
7. **Streaming**: SSE endpoint emits token chunks and completes on `done` event.
8. **Model DB seeding**: The V5 migration inserts the provider and model; Flyway applies it on startup.

### Edge Cases
- Unsloth Studio is not running — health check fails, clear error message returned. App still starts normally; offline model is unavailable until Unsloth is started.
- Model not found in available models — `ensureModelReady()` returns false with error.
- Model loading fails — `ensureModelReady()` returns false with error.
- SSE client disconnects — clean up `SseEmitter` and associated resources.
- Concurrent requests during model loading — only one load triggered, others wait on the same `CompletableFuture`.
- Model loaded but not serving — polling loop waits up to 30 seconds for model to start serving.

### Bug Fixes Applied
- **Null prefix in streaming responses**: Fixed `JsonNode.isNull()` check in `UnslothProviderApiServiceImpl` — when the LLM sends a chunk where `content` or `reasoning_content` is JSON `null`, it was being parsed as the literal string `"null"`. Null values are now treated as empty strings.
- **Health vs model readiness**: `/api/health` returning healthy only means Unsloth service is up, not that the model is loaded. `ensureModelReady()` follows a strict flow: health → model exists → model loaded → model serving.
- **Model loading concurrency**: Uses `ConcurrentHashMap` with `CompletableFuture` to coalesce concurrent load requests. Only one thread triggers the load; others wait.
- **HTTP 401 authentication errors**: `RestClient.create()` was used without any authentication header, so all chat requests to Unsloth API failed with 401. Fixed by configuring `RestClient` with `Bearer` token via `defaultHeader`. The streaming path (`HttpURLConnection`) also lacked the `Authorization` header — added it.
- **HikariCP thread starvation**: `CompletableFuture.supplyAsync()` without a dedicated executor ran model loading (blocking for up to 60 seconds) on the common ForkJoinPool, starving HikariCP's housekeeper thread. Fixed by creating a dedicated `ExecutorService` (`unsloth-model-loader`) passed to `supplyAsync()`.
- **HikariCP pool tuning**: Added explicit HikariCP configuration to `application.yml` (`maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 20000`, `max-lifetime: 1200000`) for more predictable connection pool behavior.
- **Multi-model support**: Replaced single hardcoded `app.unsloth.model` with a configurable list `app.unsloth.models` in `application.yml`. The service now dynamically resolves the model from the conversation's `Model` entity, supporting multiple models (e.g., LFM3-1.7B and Gemma-4-E4B) with different quantizations. `ensureModelReady()` uses the requested model name instead of a fixed config value. Quant lookup is done from the config map first, falling back to a live `/v1/models` query.
- **Fully dynamic model resolution**: Removed `app.unsloth.models` config entirely. Model IDs and quantization variants are now discovered from Unsloth's `/v1/models` endpoint at runtime. A cached map (`modelQuantsCache`) is populated during `ensureModelReady()` so `buildChatRequestBody()` can resolve the quant suffix without an extra API call. Adding a new model requires only a database row change — no code or config edits.
- **Image upload for vision models**: Added `supports_vision` column to the `models` table (V9 migration). The Gemma-4-E4B model is marked as vision-capable. `SendMessageRequest` accepts `imageBase64` and `imageMimeType` fields. When an image is sent, the backend formats the last user message using the multi-content array format (`type: text` + `type: image_url`) compatible with the Unsloth API. The `supportsVision` flag is exposed in `ConversationResponse.ModelInfo` so the frontend can enable/disable the image upload button.
- **Frontend requirements documented**: The frontend agent needs to (1) add `supportsVision` to model DTO, (2) conditionally show image upload button based on `supportsVision`, (3) encode images as base64 without `data:` prefix, (4) include `imageBase64` and `imageMimeType` in message request, (5) display image thumbnails in chat history, and (6) **lock model selector** once a conversation has messages to prevent mid-conversation model switching.

# Delivery Steps

### ✓ Step 1: Refactor UnslothServerManager for Unsloth API
Replace process management with Unsloth Studio API integration.
- Update `UnslothServerManager` interface: replace `start()`, `stop()`, `isRunning()`, `resetIdleTimer()` with `checkHealth()`, `listModels()`, `getInferenceStatus()`, `loadModel()`, `unloadModel()`, `isModelLoaded()`, `ensureModelReady()`.
- Rewrite `UnslothServerManagerImpl` to call Unsloth API endpoints (`/api/health`, `/v1/models`, `/api/inference/status`, `/api/inference/load`, `/api/inference/unload`) with Bearer token auth.
- Add concurrency control for model loading via `ConcurrentHashMap<String, CompletableFuture<Boolean>>`.
- Add polling with timeout to verify model is serving after load.
- Update `application.yml`: replace `port`, `cli-path`, `idle-timeout-minutes` with `base-url`, `api-key`, `max-seq-length`.

### ✓ Step 2: Update UnslothProviderApiServiceImpl
Update the provider API implementation to use Unsloth API.
- Update `chat()` and `chatStreaming()` to call `ensureModelReady()` before each request.
- Implement `buildChatRequestBody()` to produce Unsloth format with `enable_thinking`, `enable_tools`, `enabled_tools` (web_search, python, terminal), `permission_mode`, and generation params.
- Model identifier auto-resolves quant suffix from `/v1/models` (e.g., `AtomicChat/LFM3-1.7B-Uncensored-GGUF:Q4_K_M`).
- Parse SSE chunks, separate thinking content from final answer.
- Save only the final response (non-thinking) to the database after streaming completes.

### ✓ Step 3: Handle model loading concurrency and health/readiness separation
Ensure robust model loading behavior.
- Concurrent requests during model loading coalesce to a single load operation via `CompletableFuture`.
- `ensureModelReady()` follows the exact flow: health → model exists → loaded → serving → load if needed.
- `verifyModelServing()` checks the `serving` list from `/api/inference/status` — stricter than `isModelLoaded()`.
- Polling loop (up to 30 seconds, 1-second intervals) after load to verify model is serving.
- Overall load timeout is 60 seconds.