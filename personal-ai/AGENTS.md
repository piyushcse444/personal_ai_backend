# Personal AI Backend - Agent Guidelines

## Project Overview
Spring Boot 4.1.2 backend for a personal AI chat application supporting multiple AI providers (OpenAI, Anthropic, Google AI, Mistral AI, Ollama, Llama.cpp) with JWT authentication, refresh token rotation, conversations, message history, and SSE (Server-Sent Events) streaming for the local offline model.

## Tech Stack
- **Java 21** with **Spring Boot 4.1.2**
- **PostgreSQL 18+** with **Flyway** migrations
- **JPA/Hibernate 6** with **Lombok**
- **Spring Security** with **JJWT 0.12.6** (JWT access + refresh tokens)
- **BCrypt** for password hashing

## Configuration

### Server Binding
The backend binds to all network interfaces by default:

```yaml
server:
  address: 0.0.0.0
  port: 8080
```

This allows the backend to accept connections from other machines on the network (not just `localhost`). The frontend must connect to the backend's actual IP address (e.g., `http://10.196.169.181:8080`) rather than `localhost` when accessed from a different machine.

### CORS (Cross-Origin Resource Sharing)
The frontend URL is configured in `src/main/resources/application.yml`:

```yaml
app:
  cors:
    allowed-origins: http://localhost:5173, http://10.196.169.181:5173
```

To change the frontend URL, update `app.cors.allowed-origins` in `application.yml`. Multiple origins can be specified as a comma-separated list (e.g., `http://localhost:5173,http://localhost:3000`). No code changes are required.

`SecurityConfig.java` parses the comma-separated list, trims whitespace, and applies the CORS configuration to all endpoints (`/**`). Preflight requests are cached for 1 hour (`setMaxAge(3600L)`).

### JWT Tokens
```yaml
app:
  jwt:
    secret: your-256-bit-secret-here
    expiration-ms: 86400000        # 24 hours (access token)
    refresh-expiration-ms: 604800000  # 7 days (refresh token)
```

Replace the default secret with a strong random value in production.

### Database Connection
Database connection is configured in `src/main/resources/application.yml` under `spring.datasource`.

## API Endpoints (35 total)

### Public Endpoints (No Authentication Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/signup` | Register a new user |
| `POST` | `/api/auth/login` | Authenticate and receive JWT tokens |
| `POST` | `/api/auth/refresh` | Rotate refresh token, get new access token |
| `POST` | `/api/auth/logout` | Revoke current session |
| `GET` | `/api/health` | Health check |
| `GET` | `/api/providers` | List active providers |
| `GET` | `/api/providers/{id}` | Provider details with models |
| `POST` | `/api/providers` | Create a new provider |
| `DELETE` | `/api/providers/{id}` | Delete a provider |
| `GET` | `/api/providers/{id}/models` | Active models for a provider |
| `GET` | `/api/models` | List all active models (optional `?providerId=`) |
| `GET` | `/api/models/{id}` | Model details including context window |
| `POST` | `/api/models` | Create a new model |
| `DELETE` | `/api/models/{id}` | Delete a model |
| `DELETE` | `/api/models` | Delete all models |

### Authenticated Endpoints (JWT Bearer Token Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/logout-all` | Revoke all user sessions |
| `GET` | `/api/users/me` | Get current user profile |
| `PATCH` | `/api/users/me` | Update display name |
| `DELETE` | `/api/users/me` | Delete account |
| `POST` | `/api/provider-configs` | Add provider config (API key optional for offline models) |
| `GET` | `/api/provider-configs` | List user's configs (optional `?providerId=`) |
| `GET` | `/api/provider-configs/{id}` | Get one config (no secret exposed) |
| `PATCH` | `/api/provider-configs/{id}` | Update config name, endpoint, status, or credential |
| `DELETE` | `/api/provider-configs/{id}` | Delete config (if no conversation references it) |
| `POST` | `/api/provider-configs/{id}/test` | Test config connectivity |
| `POST` | `/api/conversations` | Start a conversation |
| `GET` | `/api/conversations` | List conversations (paginated, optional `?search=`) |
| `GET` | `/api/conversations/{id}` | Conversation details |
| `PATCH` | `/api/conversations/{id}` | Rename or change model/config |
| `DELETE` | `/api/conversations/{id}` | Delete conversation (auto-archived first) |
| `GET` | `/api/conversations/{id}/messages` | Message history (paginated) |
| `POST` | `/api/conversations/{id}/messages` | Send user message, call provider API, get assistant reply (both messages saved to DB) |

### Archive Endpoints (Authenticated)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/archives` | List archived conversations for current user |
| `GET` | `/api/archives/{id}/messages` | Get messages for an archived conversation |
| `DELETE` | `/api/archives/{id}` | Permanently delete an archived conversation |

### Streaming Endpoint (SSE)
| `POST` | `/api/conversations/{id}/messages/stream` | Stream assistant reply via SSE (token-by-token, thinking controlled by `thinking` request param; only the final response without thinking content is saved to DB after streaming completes) |

### Error Page
- `GET /error` — Spring Boot default error handler (public, returns structured JSON errors)

## Authentication Flow

1. **Signup**: `POST /api/auth/signup` with email, displayName, password → user created with BCrypt-hashed password
2. **Login**: `POST /api/auth/login` with email, password → returns `token` (JWT access, 24h) + `refreshToken` (7h, stored as hash in AuthSession)
3. **Protected Requests**: Include `Authorization: Bearer <token>` header; `JwtFilter` validates and sets `SecurityContextHolder`
4. **Refresh**: `POST /api/auth/refresh` with refreshToken → rotates refresh token and issues new access token
5. **Logout**: `POST /api/auth/logout` with refreshToken → revokes current session; `POST /api/auth/logout-all` → revokes all sessions

## Seeded Data

Database is pre-seeded via Flyway migrations `V2__seed_providers_and_models.sql`, `V3__add_qwen3_models.sql`, and `V5__add_llamacpp_provider_and_model.sql`:
- **6 Providers**: OpenAI, Anthropic, Google AI, Mistral AI, Ollama, Llama.cpp (Local)
- **21+ Models**: gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo, Claude Sonnet 4, Claude Opus 4, Claude Haiku 4, Gemini 2.5 Pro, Gemini 2.5 Flash, Mistral Large, Codestral, Llama 3.3, Phi 3, Qwen3 8B, Qwen3 32B, Qwen3 0.5B, LFM3-1.7B Uncensored (Offline), etc.
- **Offline Models**: All Ollama models are marked `isOffline: true` — they don't require an API key. The Llama.cpp model (`LFM3-1.7B Uncensored`) is also `isOffline: true` and runs locally via `llama-cli` on port 9901.

## Offline Models

Models have an `isOffline` boolean field. When `true`:
- No API key is required when creating a provider config
- The model runs locally (e.g., Ollama on `localhost:11434`, Llama.cpp on `localhost:9901`)
- Only an `endpointUrl` is needed in the provider config (or none at all for the built-in Llama.cpp provider)

The `isOffline` field is exposed in all model API responses so the frontend can conditionally hide the API key input field.

### Llama.cpp Offline Model
The `LFM3-1.7B Uncensored (Offline)` model is seeded via `V5__add_llamacpp_provider_and_model.sql` with `isOffline=true` and `contextWindow=8192`. It requires `llama-cli` to be installed and available on the system PATH. The server starts lazily on the first chat request and shuts down after 30 minutes of idle time. If `llama-cli` is not installed, the app still starts normally; chat requests return a clear error message.

**Request Payload**: The Llama.cpp provider sends a structured JSON request with the model name, full conversation history, 3 function tools (`get_datetime`, `run_javascript`, `get_info`), `reasoning_format: "auto"`, `chat_template_kwargs.enable_thinking` (bound to the `thinking` request param), `reasoning_control: true`, `backend_sampling: false`, and `timings_per_token: true`.

### Optional Provider Config
For offline models (like Llama.cpp), the `providerConfigId` field in `POST /api/conversations` is optional. The backend looks up the provider from the model directly when no config is provided.

## Project Structure

```
src/main/java/com/personal_ai/backend/
├── BackendApplication.java
├── config/
│   ├── SecurityConfig.java      # Security, CORS, BCrypt bean
│   └── JwtFilter.java           # OncePerRequestFilter for Bearer tokens
├── controller/
│   ├── AuthController.java      # /auth/refresh, /auth/logout, /auth/logout-all
│   ├── ArchiveController.java   # /archives (archived conversation history)
│   ├── ConversationController.java  # /conversations CRUD + /messages
│   ├── HealthController.java    # /api/health
│   ├── ProviderConfigController.java  # /provider-configs CRUD + /test
│   ├── ProviderController.java  # /providers, /models (CRUD)
│   └── UserController.java      # /auth/signup, /auth/login, /users/me
├── dto/                         # 13 DTOs for requests/responses
├── model/                       # 9 JPA entities (User, AuthSession, Provider, Model, ProviderConfig, Conversation, Message, ArchiveConversation, ArchiveMessage)
├── repository/                  # 9 Spring Data JPA repositories
├── service/
│   ├── archive/                 # Archive service for preserving chat history
│   ├── conversation/            # Conversation & Message service interfaces
│   ├── model/                   # Model & Llama.cpp provider service interfaces
│   │   ├── impl/
│   │   │   ├── LlamaCppProviderApiServiceImpl.java  # Llama.cpp provider API (sync + SSE streaming)
│   │   │   └── LlamaCppServerManagerImpl.java       # Process lifecycle manager for llama-cli
│   └── provider/                # Provider API service interfaces
└── service/impl/                # 13 service implementations (8 core + 5 provider APIs)
```

## Code Conventions

- **UUID Generation**: Use `@GeneratedValue(strategy = GenerationType.UUID)` (not deprecated `@GenericGenerator`)
- **Lombok Builders**: Use `@Builder.Default` on fields with initializing expressions (`= true`, `= new ArrayList<>()`)
- **Password Hashing**: Always hash with BCrypt before storage via `BCryptPasswordEncoder`
- **JSON Serialization**: Use `@JsonIgnore` on `@ManyToOne` relationships to prevent circular references
- **Ownership Enforcement**: All conversation, provider-config, and message endpoints verify the resource belongs to the authenticated user via `findByUserIdAndId()`
- **Credential Masking**: Never return raw credentials; use `credentialConfigured` (boolean) and `credentialMasked` (`****last4chars`) in responses
- **User ID Extraction**: Extract from `SecurityContextHolder` in each authenticated endpoint
- **Pagination**: Use Spring Data `Pageable` with `?page=0&size=20` query params

## Security Notes

- Passwords are hashed with BCrypt before storage
- JWT access tokens expire after 24 hours; refresh tokens after 7 days
- Refresh tokens stored as SHA-256 hashes in `auth_sessions` table
- Refresh token rotation: old token revoked, new token issued on each refresh
- Provider config credentials stored as ciphertext; never returned in API responses
- All user-owned resources (conversations, configs, messages) enforce ownership checks
- CORS configured via `app.cors.allowed-origins` in `application.yml`

## Provider API Integration

The send-message endpoint (`POST /api/conversations/{id}/messages`) calls the real AI provider API:
1. Fetches conversation history for context
2. Selects the correct provider implementation via `ProviderApiServiceFactory`
3. Calls the provider's API with the conversation's model and config
4. Saves and returns the assistant response with token counts

**Supported providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama, Llama.cpp (local offline).
Each provider has its own implementation class handling API-specific request/response formats.
Custom `endpointUrl` in provider configs overrides the default base URL, enabling proxy/self-hosted setups.

The `llamacpp` provider manages a local `llama-cli` process on port 9901, lazily started on first request and shut down after an idle timeout (default 30 min). It supports SSE streaming with thinking/reasoning visibility.

## Known Limitations

- **Credential Encryption**: Provider config API keys are stored as plain byte arrays; encryption at rest is planned.
- **Streaming**: SSE streaming is available for the Llama.cpp provider via `POST /api/conversations/{id}/messages/stream`. After streaming completes, only the final assistant response (without thinking content) is saved to the message table. Thinking content is streamed to the frontend but not persisted. Other providers use synchronous responses only.
- **Provider API Timeouts**: Long-running model responses may hit the 2-minute timeout (5 minutes for Ollama).
- **Llama.cpp Dependencies**: The offline model requires `llama-cli` to be installed and the model (`AtomicChat/LFM3-1.7B-Uncensored-GGUF:AD-Q`) downloaded. Server startup is lazy — the app boots normally even without it.

## Security Configuration Notes

- **SSE Streaming and Async Security**: The streaming endpoint (`/api/conversations/*/messages/stream`) runs on an async thread. Spring Security's `SecurityConfig` enables `requireExplicitSave(false)` for async context propagation and permits all requests on the stream path to avoid `AuthorizationDeniedException` on the async dispatch thread. The endpoint still validates the JWT token and conversation ownership in the controller before creating the `SseEmitter`.
- **CORS**: Configured via `app.cors.allowed-origins` in `application.yml`. Origins are parsed with whitespace trimming. Exposed headers include `*` for SSE support. Preflight cache is set to 1 hour.
- **Message Save Race Condition**: `MessageServiceImpl.createMessage()` relies on database foreign key constraints (not `existsById` checks) to validate conversation/model existence. This eliminates race conditions where a conversation is deleted (archived) between a check and the save in an async streaming thread.

## Bug Fixes

- **Null prefix in streaming responses**: Fixed `JsonNode.isNull()` check in `LlamaCppProviderApiServiceImpl` — when the LLM sends a chunk where `content` or `reasoning_content` is JSON `null`, it was being parsed as the literal string `"null"`. Null values are now treated as empty strings.
- **SSE AuthorizationDeniedException**: Fixed by enabling async security context propagation and adding `permitAll` for the stream endpoint in `SecurityConfig`.
- **Conversation not found during streaming**: Removed race-prone `existsById` check in `MessageServiceImpl.createMessage()` — database foreign key constraints now validate existence atomically at save time.

## Database Migrations

Flyway migrations in `src/main/resources/db/migration/`:
- `V1__init_schema.sql` — Core schema (users, auth_sessions, providers, models, provider_configs, conversations, messages)
- `V2__seed_providers_and_models.sql` — Seeded 6 providers and initial models
- `V3__add_qwen3_models.sql` — Added Qwen3 models
- `V5__add_llamacpp_provider_and_model.sql` — Added Llama.cpp provider and LFM3-1.7B model
- `V7__create_archive_tables.sql` — Created `archive_conversation` and `archive_message` tables for preserving chat history
