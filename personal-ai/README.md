# Personal AI Backend

A Spring Boot backend for a personal AI chat application that supports multiple AI providers (OpenAI, Anthropic, Google AI, Mistral AI, Ollama, Llama.cpp) with user authentication, conversations, message history, and SSE streaming for the local offline model.

## Tech Stack

- **Java 21** with **Spring Boot 4.1.2**
- **PostgreSQL 18+** as the primary database
- **JPA/Hibernate 6** for ORM
- **Flyway** for database migrations
- **Spring Security** with JWT authentication
- **JJWT 0.12.6** for JWT token generation and validation
- **Lombok** for reducing boilerplate code
- **BCrypt** for password hashing

## Features

- **User Authentication**: Signup, login with JWT access tokens (24h) and refresh tokens (7 days)
- **Session Management**: Refresh token rotation, single logout, logout-all
- **User Profile**: Get, update, and delete account via `/users/me`
- **Provider Catalog**: Read-only access to 6 seeded providers (OpenAI, Anthropic, Google AI, Mistral AI, Ollama, Llama.cpp) with 21+ models, including offline models
- **Offline Models**: Models marked as `isOffline` (e.g., Ollama/Qwen3, Llama.cpp) don't require an API key — just configure the endpoint URL or use the built-in Llama.cpp provider
- **Provider Configs**: Per-user credential management with API key masking; API key is optional for offline models
- **Conversations**: Full CRUD with ownership enforcement, title search, and pagination; `providerConfigId` is optional for offline models; deleted conversations are automatically archived for future model training
- **Messages**: Message history and send-message endpoint with real AI provider API integration; both user and assistant messages are saved to the database. For streaming, only the final assistant response (without thinking content) is saved after streaming completes; thinking content is streamed to the frontend but not persisted.
- **Chat Archive**: When a conversation is deleted, it is archived to `archive_conversation` and `archive_message` tables using string-based references (userEmail, providerName, modelName) so the data persists even if the user, provider, or model is later deleted
- **SSE Streaming**: Real-time token streaming with thinking/reasoning visibility for the Llama.cpp provider via `POST /api/conversations/{id}/messages/stream`; thinking can be toggled on/off via the `thinking` request parameter
- **CORS Support**: Configurable multi-origin frontend access via `application.yml`; server binds to `0.0.0.0` for network access

## Core Domains

The application manages seven core entities:

1. **Users** — Registration, authentication, and account management
2. **Auth Sessions** — Refresh token-based session management with rotation and revocation
3. **Providers** — Catalog of AI providers (seeded via Flyway migration)
4. **Models** — AI models available from each provider with context window info
5. **Provider Configs** — Per-user provider credentials and endpoint settings
6. **Conversations** — User chat sessions linked to a model and provider config
7. **Messages** — Individual messages within conversations (USER/ASSISTANT roles)
8. **Archive Conversation** — Archived chat history (independent of users/providers/models, uses string references)
9. **Archive Message** — Archived messages linked to archive conversations

## Database Design

See [`DATABASE_DESIGN.md`](../DATABASE_DESIGN.md) for the complete database schema, relationships, constraints, and security guidelines.

## Getting Started

### Prerequisites
- Java 21+
- PostgreSQL 18+
- Maven 3.8+

### Configuration

Set up your database connection in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/personal-ai
    username: your_username
    password: your_password
```

#### Server Binding

The server binds to all network interfaces by default:

```yaml
server:
  address: 0.0.0.0
  port: 8080
```

This allows the backend to accept connections from other machines on the network. When accessing from another machine, use the backend's actual IP address (e.g., `http://10.196.169.181:8080`) instead of `localhost`.

#### CORS (Frontend URL)

The allowed frontend origins are configured in `application.yml`:

```yaml
app:
  cors:
    allowed-origins: http://localhost:5173, http://10.196.169.181:5173
```

To change the frontend URL, update `app.cors.allowed-origins`. Multiple origins can be specified as a comma-separated list (e.g., `http://localhost:5173,http://localhost:3000`). No code changes are needed. Origins are parsed with whitespace trimming, and preflight requests are cached for 1 hour.

#### JWT Configuration

```yaml
app:
  jwt:
    secret: your-256-bit-secret-here
    expiration-ms: 86400000        # 24 hours
    refresh-expiration-ms: 604800000  # 7 days
```

**Important**: Replace the default JWT secret with a strong random value in production.

### Running the Application

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`. Database migrations are applied automatically via Flyway.

### Running Tests

```bash
./mvnw test
```

## Project Structure

```
src/main/java/com/personal_ai/backend/
├── BackendApplication.java           # Main application entry point
├── config/                           # Configuration classes
│   ├── SecurityConfig.java           # Spring Security, CORS, BCrypt config
│   └── JwtFilter.java                # JWT Bearer token filter
├── controller/                       # REST controllers
│   ├── AuthController.java           # Refresh, logout, logout-all
│   ├── ConversationController.java   # Conversations CRUD + messages
│   ├── HealthController.java         # Health check endpoint
│   ├── ProviderConfigController.java # Provider config CRUD + test
│   ├── ArchiveController.java        # Archived conversation history
│   ├── ProviderController.java       # Provider & model CRUD endpoints
│   └── UserController.java           # Signup, login, /users/me
├── dto/                              # Data transfer objects
│   ├── CreateConversationRequest.java
│   ├── CreateModelRequest.java
│   ├── CreateProviderConfigRequest.java
│   ├── CreateProviderRequest.java
│   ├── LoginRequest.java
│   ├── MessageResponse.java
│   ├── RefreshRequest.java
│   ├── SendMessageRequest.java
│   ├── SignupRequest.java
│   ├── UpdateConversationRequest.java
│   ├── UpdateProviderConfigRequest.java
│   ├── UpdateUserRequest.java
│   └── ProviderConfigResponse.java
├── model/                            # JPA entity classes
│   ├── ArchiveConversation.java
│   ├── ArchiveMessage.java
│   ├── AuthSession.java
│   ├── Conversation.java
│   ├── Message.java
│   ├── Model.java
│   ├── Provider.java
│   ├── ProviderConfig.java
│   └── User.java
├── repository/                       # Spring Data JPA repositories
│   ├── ArchiveConversationRepository.java
│   ├── ArchiveMessageRepository.java
│   ├── AuthSessionRepository.java
│   ├── ConversationRepository.java
│   ├── MessageRepository.java
│   ├── ModelRepository.java
│   ├── ProviderConfigRepository.java
│   ├── ProviderRepository.java
│   └── UserRepository.java
├── service/                          # Service interfaces
│   ├── archive/                      # Archive service for chat history
│   ├── conversation/                 # Conversation & Message services
│   ├── model/                        # Model & Llama.cpp services
│   │   └── impl/
│   │       ├── LlamaCppProviderApiServiceImpl.java  # Llama.cpp API (sync + SSE streaming)
│   │       └── LlamaCppServerManagerImpl.java       # llama-cli process manager
│   └── provider/                     # Provider API services
└── service/impl/                     # Service implementations
    ├── AuthServiceImpl.java
    ├── AuthSessionServiceImpl.java
    ├── AnthropicProviderApiServiceImpl.java
    ├── ConversationServiceImpl.java
    ├── GoogleProviderApiServiceImpl.java
    ├── MessageServiceImpl.java
    ├── MistralProviderApiServiceImpl.java
    ├── ModelServiceImpl.java
    ├── OllamaProviderApiServiceImpl.java
    ├── OpenAiProviderApiServiceImpl.java
    ├── ProviderConfigServiceImpl.java
    ├── ProviderServiceImpl.java
    └── UserServiceImpl.java
```

## API Endpoints

See [`API.md`](./API.md) for complete API documentation with request/response examples.

### Quick Reference

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/auth/signup` | ✗ | Register a new user |
| `POST` | `/api/auth/login` | ✗ | Authenticate and receive tokens |
| `POST` | `/api/auth/refresh` | ✗ | Rotate refresh token |
| `POST` | `/api/auth/logout` | ✗ | Revoke current session |
| `GET` | `/api/health` | ✗ | Health check |
| `GET` | `/api/providers` | ✗ | List active providers |
| `GET` | `/api/providers/{id}` | ✗ | Provider details |
| `POST` | `/api/providers` | ✗ | Create a new provider |
| `DELETE` | `/api/providers/{id}` | ✗ | Delete a provider |
| `GET` | `/api/providers/{id}/models` | ✗ | Models for a provider |
| `GET` | `/api/models` | ✗ | List all active models |
| `GET` | `/api/models/{id}` | ✗ | Model details |
| `POST` | `/api/models` | ✗ | Create a new model |
| `DELETE` | `/api/models/{id}` | ✗ | Delete a model |
| `DELETE` | `/api/models` | ✗ | Delete all models |
| `POST` | `/api/auth/logout-all` | ✓ | Revoke all sessions |
| `GET` | `/api/users/me` | ✓ | Get current user profile |
| `PATCH` | `/api/users/me` | ✓ | Update display name |
| `DELETE` | `/api/users/me` | ✓ | Delete account |
| `POST` | `/api/provider-configs` | ✓ | Add provider config (API key optional for offline models) |
| `GET` | `/api/provider-configs` | ✓ | List user's configs |
| `GET` | `/api/provider-configs/{id}` | ✓ | Get one config (no secret) |
| `PATCH` | `/api/provider-configs/{id}` | ✓ | Update config |
| `DELETE` | `/api/provider-configs/{id}` | ✓ | Delete config |
| `POST` | `/api/provider-configs/{id}/test` | ✓ | Test connectivity |
| `POST` | `/api/conversations` | ✓ | Start a conversation |
| `GET` | `/api/conversations` | ✓ | List conversations (paginated) |
| `GET` | `/api/conversations/{id}` | ✓ | Conversation details |
| `PATCH` | `/api/conversations/{id}` | ✓ | Rename or change model/config |
| `DELETE` | `/api/conversations/{id}` | ✓ | Delete conversation (auto-archived first) |
| `GET` | `/api/archives` | ✓ | List archived conversations |
| `GET` | `/api/archives/{id}/messages` | ✓ | Archived conversation messages |
| `DELETE` | `/api/archives/{id}` | ✓ | Delete archived conversation |
| `GET` | `/api/conversations/{id}/messages` | ✓ | Message history |
| `POST` | `/api/conversations/{id}/messages` | ✓ | Send a message and get AI response (both saved to DB) |
| `POST` | `/api/conversations/{id}/messages/stream` | ✓ | Stream AI response via SSE (Llama.cpp; saved to DB after streaming) |

## Authentication Flow

1. **Signup**: `POST /api/auth/signup` with email, display name, and password
2. **Login**: `POST /api/auth/login` with email and password → receives JWT access token + refresh token
3. **Authenticated Requests**: Include `Authorization: Bearer <token>` header
4. **Token Refresh**: `POST /api/auth/refresh` with refresh token when access token expires (24h)
5. **Logout**: `POST /api/auth/logout` to revoke current session or `POST /api/auth/logout-all` to revoke all

## Seeded Data

The database is pre-seeded with 6 providers and 18+ models via Flyway migrations `V2__seed_providers_and_models.sql`, `V3__add_qwen3_models.sql`, and `V5__add_llamacpp_provider_and_model.sql`:

| Provider | Type | Models |
|----------|------|--------|
| OpenAI | Cloud | gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo |
| Anthropic | Cloud | Claude Sonnet 4, Claude Sonnet 4 (Thinking), Claude Opus 4, Claude Haiku 4 |
| Google AI | Cloud | Gemini 2.5 Pro, Gemini 2.5 Flash, Gemini 2.5 Flash Lite |
| Mistral AI | Cloud | Mistral Large, Mistral Small, Codestral |
| Ollama | Self-hosted (offline) | Llama 3.3, Mistral (Local), Phi 3, Qwen3 8B, Qwen3 32B, Qwen3 0.5B |
| Llama.cpp | Local (offline) | LFM3-1.7B Uncensored (Offline) |

## Offline Models

Models can be marked as offline (`isOffline: true`) via the `is_offline` column in the `models` table. Offline models:
- **Do not require an API key** when creating a provider config
- Are typically self-hosted (e.g., Ollama running on localhost, Llama.cpp on localhost:9901)
- Only need an `endpointUrl` pointing to the local server (or none at all for the built-in Llama.cpp provider)

All Ollama models and the Llama.cpp model are pre-marked as offline. Cloud providers (OpenAI, Anthropic, Google AI, Mistral AI) require an API key.

### Llama.cpp Local Model
The `LFM3-1.7B Uncensored (Offline)` model runs locally via `llama-cli` on port 9901. It starts lazily on the first chat request and shuts down after 30 minutes of idle time. The app boots normally even if `llama-cli` is not installed — chat requests will return a clear error message until it is available.

**Request Payload**: The Llama.cpp provider sends a structured JSON request including the model name, full conversation history, 3 function tools (`get_datetime`, `run_javascript`, `get_info`), reasoning controls (`reasoning_format: "auto"`, `chat_template_kwargs.enable_thinking` bound to the `thinking` request parameter), `reasoning_control: true`, `backend_sampling: false`, and `timings_per_token: true`.

### Optional Provider Config
For offline models, the `providerConfigId` field in `POST /api/conversations` is optional. The backend looks up the provider from the model directly when no config is provided.

## Security Notes

- Passwords are hashed using BCrypt before storage
- JWT access tokens expire after 24 hours; refresh tokens expire after 7 days
- Refresh tokens are stored as SHA-256 hashes in the database
- Refresh token rotation on each refresh (old token revoked, new token issued)
- Provider config credentials are stored as ciphertext; API responses mask credentials (`****last4chars`)
- Ownership enforcement: conversations, configs, and messages are scoped to the authenticated user
- CORS is configured via `app.cors.allowed-origins` in `application.yml`
- API credential encryption is planned for a future release

## Provider API Integration

The send-message endpoint (`POST /api/conversations/{id}/messages`) calls the real AI provider API:
1. Fetches conversation history for context
2. Selects the correct provider implementation via `ProviderApiServiceFactory`
3. Calls the provider's API using the conversation's model and config
4. Saves and returns the assistant response with token counts

**Supported providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama, Llama.cpp (local offline).
Each provider has its own implementation class handling API-specific request/response formats.
Custom `endpointUrl` in provider configs overrides the default base URL, enabling proxy/self-hosted setups.

The `llamacpp` provider manages a local `llama-cli` process on port 9901, lazily started on first request and shut down after an idle timeout (default 30 min). It supports SSE streaming with thinking/reasoning visibility.

## Known Bug Fixes

- **Null prefix in streaming responses**: Fixed `JsonNode.isNull()` check in `LlamaCppProviderApiServiceImpl` — when the LLM sends a chunk where `content` or `reasoning_content` is JSON `null`, it was being parsed as the literal string `"null"`. Null values are now treated as empty strings.
- **SSE AuthorizationDeniedException**: Fixed by enabling async security context propagation and adding `permitAll` for the stream endpoint in `SecurityConfig`.
- **Conversation not found during streaming**: Removed race-prone `existsById` check in `MessageServiceImpl.createMessage()` — database foreign key constraints now validate existence atomically at save time.

## Future Work

- Streaming responses for other providers (currently only Llama.cpp supports SSE; only the final assistant response without thinking content is saved to the message table after streaming completes)
- Message editing and regeneration
- Conversation sharing
- Admin endpoints for provider/model catalog management
- Usage analytics and rate limiting
- API credential encryption at rest
- Support for additional AI providers
