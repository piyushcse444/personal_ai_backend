# Personal AI Backend - API Documentation

Base URL: `http://localhost:8080`

**Note**: The server binds to `0.0.0.0` (all network interfaces) by default (`server.address: 0.0.0.0` in `application.yml`). When accessing from another machine on the network, use the backend's actual IP address (e.g., `http://10.196.169.181:8080`) instead of `localhost`.

---

## Public Endpoints (No Authentication Required)

### 1. `POST /api/auth/signup`

Register a new user. Password is hashed with BCrypt before storage.

**Request Body:**
```json
{
  "email": "user@example.com",
  "displayName": "John Doe",
  "password": "securepassword123"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "John Doe"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Email already in use"
}
```

---

### 2. `POST /api/auth/login`

Authenticate with email and password. Returns JWT access token and refresh token.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "securepassword123"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC...",
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Invalid email or password"
}
```

---

### 3. `POST /api/auth/refresh`

Rotate refresh token and issue a new access token.

**Request Body:**
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI1NTBlODQwMC...",
  "refreshToken": "new-refresh-token-uuid"
}
```

---

### 4. `POST /api/auth/logout`

Revoke the current refresh token session.

**Request Body:**
```json
{
  "refreshToken": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```

---

### 5. `GET /api/health`

Health check endpoint to verify the service is running.

**Success Response (200 OK):**
```json
{
  "status": "UP",
  "service": "personal-ai-backend",
  "timestamp": "2026-11-01T10:00:00Z"
}
```

---

### 6. `GET /api/providers`

List all active AI providers.

**Success Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "openai",
    "displayName": "OpenAI",
    "providerType": "CLOUD",
    "baseUrl": "https://api.openai.com",
    "isActive": true
  }
]
```

---

### 7. `GET /api/providers/{id}`

Get details for a specific provider.

**Success Response (200 OK):** Provider object with nested models list.

---

### 8. `POST /api/providers`

Create a new AI provider in the system.

**Request Body:**
```json
{
  "code": "openai",
  "displayName": "OpenAI",
  "providerType": "CLOUD",
  "baseUrl": "https://api.openai.com/v1"
}
```

**Field descriptions:**
- `code` (required) — Unique identifier code for the provider (e.g., "openai", "anthropic")
- `displayName` (required) — Human-readable display name for the provider
- `providerType` (required) — Type of the provider: `CLOUD`, `LOCAL`, or `SELF_HOSTED`
- `baseUrl` (optional) — Base URL for the provider API endpoint

**Success Response (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "code": "openai",
  "displayName": "OpenAI",
  "providerType": "CLOUD",
  "baseUrl": "https://api.openai.com/v1",
  "isActive": true,
  "createdAt": "2026-11-13T10:00:00Z",
  "updatedAt": "2026-11-13T10:00:00Z"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Provider code already exists: openai"
}
```

---

### 9. `DELETE /api/providers/{id}`

Permanently delete an AI provider from the system.

**Path Parameter:**
- `id` — UUID of the provider to delete

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Provider deleted successfully"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Provider not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 10. `GET /api/providers/{id}/models`

List active models for a specific provider.

**Success Response (200 OK):**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "modelName": "gpt-4",
    "displayName": "GPT-4",
    "contextWindow": 8192,
    "isActive": true
  }
]
```

---

### 11. `GET /api/models`

List all active models, optionally filtered by provider.

**Query Params:**
- `providerId` (optional) — filter by provider UUID

**Success Response (200 OK):** Array of model objects. Each model includes an `isOffline` field:
- `isOffline: true` — the model runs locally (e.g., Ollama). No API key is required.
- `isOffline: false` — the model is a cloud service. An API key is required when creating a provider config.

---

### 12. `GET /api/models/{id}`

Get details for a specific model.

**Success Response (200 OK):** Model object with contextWindow and isOffline info.

**Example response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "modelName": "qwen3:8b",
  "displayName": "Qwen3 8B",
  "contextWindow": 131072,
  "isActive": true,
  "isOffline": true
}
```

---

### 13. `POST /api/models`

Create a new AI model under an existing provider.

**Request Body:**
```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440000",
  "modelName": "gpt-4",
  "displayName": "GPT-4",
  "contextWindow": 8192
}
```

**Field descriptions:**
- `providerId` (required) — UUID of the provider this model belongs to
- `modelName` (required) — Unique model identifier (e.g., "gpt-4", "claude-3-opus")
- `displayName` (required) — Human-readable display name for the model
- `contextWindow` (required) — Context window size in tokens (must be positive)

**Success Response (201 Created):**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "providerId": "550e8400-e29b-41d4-a716-446655440000",
  "modelName": "gpt-4",
  "displayName": "GPT-4",
  "contextWindow": 8192,
  "isActive": true,
  "createdAt": "2026-11-13T10:00:00Z",
  "updatedAt": "2026-11-13T10:00:00Z"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Provider not found: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 14. `DELETE /api/models/{id}`

Permanently delete an AI model from the system.

**Path Parameter:**
- `id` — UUID of the model to delete

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Model deleted successfully"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Model not found: 660e8400-e29b-41d4-a716-446655440001"
}
```

---

### 15. `DELETE /api/models`

Delete all models from the system.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "All models deleted successfully",
  "deletedCount": 5
}
```

---

## Authenticated Endpoints

All endpoints below require a valid JWT Bearer token in the `Authorization` header.

**Required Header:**
```
Authorization: Bearer <your-jwt-token>
```

### Authentication & Account

#### `POST /api/auth/logout-all`

Revoke all sessions for the authenticated user.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "All sessions revoked"
}
```

#### `GET /api/users/me`

Get the current user's profile.

**Success Response (200 OK):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "John Doe"
}
```

#### `PATCH /api/users/me`

Update the current user's display name.

**Request Body:**
```json
{
  "displayName": "New Name"
}
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "displayName": "New Name"
}
```

#### `DELETE /api/users/me`

Delete the current user's account.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Account deleted successfully"
}
```

---

### Provider Configs

#### `POST /api/provider-configs`

Add a provider config. The `apiKey` field is **optional** — it is only required for online (cloud) models. For offline models (e.g., Ollama), omit the `apiKey`.

**Request Body:**
```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440000",
  "configName": "My OpenAI Config",
  "apiKey": "sk-abc123...",
  "endpointUrl": "https://api.openai.com/v1"
}
```

**For offline models (no API key needed):**
```json
{
  "providerId": "e5f6a7b8-c9d0-1234-efab-345678901234",
  "configName": "My Ollama Config",
  "endpointUrl": "http://localhost:11434"
}
```

**Success Response (201 Created):**
```json
{
  "id": "config-uuid",
  "configName": "My OpenAI Config",
  "isActive": true,
  "provider": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "code": "openai",
    "displayName": "OpenAI"
  },
  "credentialConfigured": true,
  "credentialMasked": "****c123"
}
```

#### `GET /api/provider-configs`

List the current user's provider configs.

**Query Params:**
- `providerId` (optional) — filter by provider UUID

**Success Response (200 OK):** Array of config response objects.

#### `GET /api/provider-configs/{id}`

Get one of the current user's configs (no secret exposed).

**Success Response (200 OK):** Single config response object.

#### `PATCH /api/provider-configs/{id}`

Update a config's name, endpoint, status, or replace its credential.

**Request Body:**
```json
{
  "configName": "Updated Name",
  "apiKey": "sk-newkey...",
  "endpointUrl": "https://api.openai.com/v1",
  "isActive": true
}
```

**Success Response (200 OK):** Updated config response object.

#### `DELETE /api/provider-configs/{id}`

Delete a config if no conversation references it.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Config deleted successfully"
}
```

#### `POST /api/provider-configs/{id}/test`

Test whether a config's credentials and endpoint can reach the provider.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Config test successful",
  "provider": "openai"
}
```

---

### Conversations

#### `POST /api/conversations`

Start a new conversation with a model and optional provider config. For offline models (e.g., Llama.cpp), `providerConfigId` is optional — the backend looks up the provider from the model directly.

**Request Body:**
```json
{
  "modelId": "model-uuid",
  "providerConfigId": "config-uuid",  // Optional for offline models
  "title": "My Conversation"
}
```

**Example for offline model (Llama.cpp):**
```json
{
  "modelId": "21c2d3e4-f5a6-7890-cdef-901234567890",
  "title": "Offline Chat"
}
```

**Success Response (201 Created):**
```json
{
  "id": "conv-uuid",
  "title": "My Conversation",
  "model": {
    "id": "model-uuid",
    "modelName": "gpt-4",
    "displayName": "GPT-4"
  },
  "updatedAt": "2026-11-01T10:00:00Z"
}
```

#### `GET /api/conversations`

List the user's conversations with optional title search.

**Query Params:**
- `search` (optional) — filter by title substring

**Success Response (200 OK):** Array of conversation response objects.

#### `GET /api/conversations/{id}`

Get conversation details.

**Success Response (200 OK):** Single conversation response object.

#### `PATCH /api/conversations/{id}`

Rename a conversation or change its model/config.

**Request Body:**
```json
{
  "title": "New Title",
  "modelId": "new-model-uuid",
  "providerConfigId": "new-config-uuid"
}
```

**Success Response (200 OK):** Updated conversation response object.

#### `DELETE /api/conversations/{id}`

Delete a conversation and its messages. **Before deleting, the conversation and all its messages are automatically archived** to `archive_conversation` and `archive_message` tables. The archived data uses string-based references (userEmail, providerName, modelName, providerConfigName) instead of foreign keys, so it persists even if the user, provider, or model is deleted later. This archived data can be used for future model training.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Conversation deleted successfully"
}
```

---

### Archive

Archived conversations are stored independently of users, providers, and models. They use string-based references instead of foreign keys, so the data persists even after the original entities are deleted.

#### `GET /api/archives`

List all archived conversations for the current user (authenticated).

**Success Response (200 OK):**
```json
[
  {
    "id": "archive-uuid",
    "userEmail": "user@example.com",
    "providerName": "OpenAI",
    "modelName": "gpt-4",
    "title": "My Conversation",
    "providerConfigName": "My OpenAI Config",
    "archivedAt": "2026-11-14T10:00:00Z",
    "messages": []
  }
]
```

#### `GET /api/archives/{archiveConversationId}/messages`

Get all messages for an archived conversation in chronological order.

**Success Response (200 OK):**
```json
[
  {
    "id": "archive-msg-uuid",
    "role": "USER",
    "content": "Hello!",
    "tokenCount": null,
    "modelName": "gpt-4",
    "archivedAt": "2026-11-14T10:00:00Z"
  },
  {
    "id": "archive-msg-uuid-2",
    "role": "ASSISTANT",
    "content": "Hi! How can I help you?",
    "tokenCount": 128,
    "modelName": "gpt-4",
    "archivedAt": "2026-11-14T10:00:01Z"
  }
]
```

**Error Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Archived conversation not found: archive-uuid"
}
```

#### `DELETE /api/archives/{archiveConversationId}`

Permanently delete an archived conversation and its messages.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Archived conversation deleted successfully"
}
```

**Error Response (404 Not Found):**
```json
{
  "success": false,
  "message": "Archived conversation not found: archive-uuid"
}
```

---

### Messages

#### `GET /api/conversations/{id}/messages`

Get message history in chronological order.

**Success Response (200 OK):**
```json
[
  {
    "id": "msg-uuid",
    "role": "USER",
    "content": "Hello!",
    "tokenCount": null,
    "createdAt": "2026-11-01T10:00:00Z"
  },
  {
    "id": "msg-uuid-2",
    "role": "ASSISTANT",
    "content": "Response pending...",
    "tokenCount": null,
    "createdAt": "2026-11-01T10:00:01Z"
  }
]
```

#### `POST /api/conversations/{id}/messages`

Send a user message and receive a real assistant reply from the AI provider.

The server fetches the conversation history, calls the provider's API using the conversation's model and config, saves both the user message and assistant response to the `message` table, and returns them.

**Supported Providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama, Llama.cpp (local offline).

#### `POST /api/conversations/{id}/messages/stream`

Stream an assistant reply in real-time using Server-Sent Events (SSE). Available for the Llama.cpp provider; other providers fall back to a synchronous response. After streaming completes, only the final assistant response (without thinking content) is automatically saved to the `message` table. Thinking content is streamed to the frontend but not persisted.

**Request Body:**
```json
{
  "content": "Explain quantum computing",
  "thinking": true
}
```

**Field descriptions:**
- `content` (required) — The user's message text
- `thinking` (optional) — Enable/disable thinking/reasoning mode. When `true` (default), reasoning-capable models will produce thinking content. When `false`, thinking is disabled at the model level and no reasoning content is generated.

**SSE Events:**
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

- `isThinking: true` indicates the model is producing reasoning/thinking steps.
- `isThinking: false` indicates the final answer content.
- The `done` event signals the end of the stream with token counts.

**Request Body (sync endpoint `POST /api/conversations/{id}/messages`):**
```json
{
  "content": "Explain what a database index is.",
  "thinking": true
}
```

**Success Response (200 OK) for sync endpoint:**
```json
{
  "success": true,
  "userMessage": {
    "id": "msg-uuid",
    "role": "USER",
    "content": "Explain what a database index is.",
    "tokenCount": null,
    "createdAt": "2026-11-01T10:00:00Z"
  },
  "assistantMessage": {
    "id": "msg-uuid-2",
    "role": "ASSISTANT",
    "content": "A database index is a data structure that improves the speed of data retrieval...",
    "tokenCount": 156,
    "createdAt": "2026-11-01T10:00:01Z"
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Failed to get response from AI provider: API key not configured"
}
```

---

## Error Page

### `GET /error`

Spring Boot default error handler. Returns structured error information for failed requests.

This endpoint is publicly accessible (no authentication required).

**Example Response (404 Not Found):**
```json
{
  "timestamp": "2026-11-01T10:00:00Z",
  "status": 404,
  "error": "Not Found",
  "path": "/api/nonexistent"
}
```

**Common Status Codes:**
| Code | Meaning |
|------|---------|
| `400` | Bad Request — invalid input or validation failure |
| `401` | Unauthorized — invalid or expired JWT token |
| `403` | Forbidden — no authentication token provided |
| `404` | Not Found — endpoint does not exist |
| `500` | Internal Server Error — unexpected server error |

---

## Authentication Flow

1. **Signup**: Call `POST /api/auth/signup` with email, display name, and password.
2. **Login**: Call `POST /api/auth/login` with email and password to receive JWT access token and refresh token.
3. **Authenticated Requests**: Include the access token in the `Authorization` header for all protected endpoints:
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
   ```
4. **Token Refresh**: When the access token expires (24 hours), call `POST /api/auth/refresh` with the refresh token to get a new pair.
5. **Logout**: Call `POST /api/auth/logout` with the refresh token to revoke the current session, or `POST /api/auth/logout-all` to revoke all sessions.

---

## Configuration

All configurable values are in `src/main/resources/application.yml`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.address` | `0.0.0.0` | Server bind address (all network interfaces) |
| `app.cors.allowed-origins` | `http://localhost:5173, http://10.196.169.181:5173` | Allowed frontend origins (comma-separated, whitespace-trimmed) |
| `app.jwt.secret` | *(placeholder)* | JWT signing secret (change in production) |
| `app.jwt.expiration-ms` | `86400000` | JWT access token lifetime in milliseconds (24 hours) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token lifetime in milliseconds (7 days) |
| `app.llamacpp.enabled` | `true` | Enable/disable the Llama.cpp local provider (Unsloth Studio) |
| `app.llamacpp.base-url` | `http://127.0.0.1:8888` | Base URL of the Unsloth Studio API |
| `app.llamacpp.api-key` | `sk-unsloth-...` | Bearer token for Unsloth API authentication |
| `app.llamacpp.model` | `AtomicChat/LFM3-1.7B-Uncensored-GGUF` | Model path/identifier in Unsloth |
| `app.llamacpp.max-seq-length` | `2048` | Maximum sequence length for model loading |

---

## Llama.cpp Local Provider (Unsloth Studio)

The `llamacpp` provider communicates with the Unsloth Studio API (running on port 8888) for model management and inference. Key behaviors:

- **Unsloth Studio API**: Instead of managing a local `llama-cli` process, the backend calls the Unsloth Studio API for health checks, model loading/unloading, and chat completions.
- **Model readiness flow**: Before each chat request, the backend ensures the model is ready: (1) check Unsloth health, (2) verify model exists, (3) check if model is loaded, (4) verify model is serving, (5) load if needed.
- **Concurrency control**: If multiple requests arrive while the model is loading, only one load operation runs. Concurrent requests wait on the same `CompletableFuture` until loading completes.
- **Health vs model readiness**: `/api/health` returning healthy only means the Unsloth service is up — it does not guarantee the model is loaded and ready for inference.
- **SSE streaming**: Use `POST /api/conversations/{id}/messages/stream` for real-time token streaming with thinking visibility.
- **No provider config needed**: The `providerConfigId` is optional when creating a conversation with the Llama.cpp model.
- **Requirements**: Unsloth Studio must be running on the configured port, and the model (`AtomicChat/LFM3-1.7B-Uncensored-GGUF`) must be available in Unsloth.

### Model Loading Flow

```
Application starts
      |
Check Unsloth health (/api/health)
      |
Check expected model exists (/v1/models)
      |
Check whether model is loaded (/api/inference/status)
      |
  +---+---+
  |       |
Loaded  Not loaded
  |       |
Verify   Load model
serving  (/api/inference/load)
  |      |
  +---+--+
      |
   Verify serving (poll up to 30s)
      |
   Ready
```

### Request Payload

The Llama.cpp provider sends a structured JSON request to the Unsloth API with the following fields:

| Field | Value | Description |
|-------|-------|-------------|
| `model` | `model_path:quant` (e.g., `AtomicChat/LFM3-1.7B-Uncensored-GGUF:Q4_K_M`) | Model identifier with quant suffix |
| `messages` | Array of `{role, content}` | Full conversation history |
| `temperature` | `0.7` | Sampling temperature |
| `top_p` | `0.8` | Nucleus sampling threshold |
| `top_k` | `20` | Top-k sampling |
| `min_p` | `0.05` | Minimum probability threshold |
| `repetition_penalty` | `1.1` | Repetition penalty factor |
| `max_tokens` | `1024` | Maximum tokens to generate |
| `enable_thinking` | `true`/`false` | Enable/disable reasoning/thinking mode (bound to `thinking` request param) |
| `enable_tools` | `true`/`false` | Enable/disable tool usage (web search, Python, terminal) |
| `enabled_tools` | Array of tool names | `["web_search", "python", "terminal"]` when tools enabled |
| `permission_mode` | `"off"` | Tool permission mode |
| `stream` | `true`/`false` | Streaming flag |

### Enabled Tools

When `enable_tools` is `true`, the following tools are available to the LLM:

1. **`web_search`** — Allows the model to search the internet for current information.
2. **`python`** — Allows the model to execute Python code.
3. **`terminal`** — Allows the model to execute terminal commands.

These tools are managed by the Unsloth Studio backend, not by the Java application.

---

## SSE Streaming Security Notes

The streaming endpoint (`POST /api/conversations/{id}/messages/stream`) runs on an async thread. Spring Security is configured to:
- Enable async security context propagation (`requireExplicitSave(false)`)
- Permit all requests on the stream path to avoid `AuthorizationDeniedException` on the async dispatch thread
- The endpoint still validates the JWT token and conversation ownership in the controller before creating the `SseEmitter`

The response includes `Access-Control-Allow-Origin` headers matching the requesting frontend origin, and `Access-Control-Expose-Headers: *` for SSE compatibility.

---

## Known Bug Fixes

- **Null prefix in streaming responses**: Fixed `JsonNode.isNull()` check in `LlamaCppProviderApiServiceImpl` — when the LLM sends a chunk where `content` or `reasoning_content` is JSON `null`, it was being parsed as the literal string `"null"`. Null values are now treated as empty strings.
- **SSE AuthorizationDeniedException**: Fixed by enabling async security context propagation and adding `permitAll` for the stream endpoint in `SecurityConfig`.
- **Conversation not found during streaming**: Removed race-prone `existsById` check in `MessageServiceImpl.createMessage()` — database foreign key constraints now validate existence atomically at save time.
- **Health vs model readiness**: `/api/health` returning healthy only means the Unsloth service is up, not that the model is loaded. `ensureModelReady()` follows a strict flow: health → model exists → model loaded → model serving.
- **Model loading concurrency**: Concurrent requests during model loading now coalesce to a single load operation via `ConcurrentHashMap` and `CompletableFuture`, preventing multiple simultaneous load requests.
- **HTTP 401 authentication errors**: `RestClient.create()` was used without any authentication header, so all chat requests to Unsloth API failed with 401. Fixed by configuring `RestClient` with `Bearer` token via `defaultHeader`. The streaming path (`HttpURLConnection`) also lacked the `Authorization` header — added it.
- **HikariCP thread starvation**: `CompletableFuture.supplyAsync()` without a dedicated executor ran model loading (blocking for up to 60 seconds) on the common ForkJoinPool, starving HikariCP's housekeeper thread and causing warnings like `Thread starvation or clock leap detected`. Fixed by creating a dedicated `ExecutorService` (`unsloth-model-loader`) passed to `supplyAsync()`.
- **HikariCP pool tuning**: Added explicit HikariCP configuration to `application.yml` (`maximum-pool-size: 20`, `minimum-idle: 5`, `connection-timeout: 20000`, `max-lifetime: 1200000`) for more predictable connection pool behavior.

---

## Database Migrations

Flyway migrations in `src/main/resources/db/migration/`:
- `V1__init_schema.sql` — Core schema (users, auth_sessions, providers, models, provider_configs, conversations, messages)
- `V2__seed_providers_and_models.sql` — Seeded 6 providers and initial models
- `V3__add_qwen3_models.sql` — Added Qwen3 models
- `V5__add_llamacpp_provider_and_model.sql` — Added Llama.cpp provider and LFM3-1.7B model
- `V7__create_archive_tables.sql` — Created `archive_conversation` and `archive_message` tables for preserving chat history
