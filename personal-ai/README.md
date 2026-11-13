# Personal AI Backend

A Spring Boot backend for a personal AI chat application that supports multiple AI providers (OpenAI, Anthropic, Google AI, Mistral AI, Ollama, etc.) with user authentication, conversations, and message history.

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
- **Provider Catalog**: Read-only access to 5 seeded providers (OpenAI, Anthropic, Google AI, Mistral AI, Ollama) with 17 models
- **Provider Configs**: Per-user credential management with API key masking and connectivity testing
- **Conversations**: Full CRUD with ownership enforcement, title search, and pagination
- **Messages**: Message history and send-message endpoint with real AI provider API integration (OpenAI, Anthropic, Google AI, Mistral AI, Ollama)
- **CORS Support**: Configurable frontend origin via `application.yml`

## Core Domains

The application manages seven core entities:

1. **Users** — Registration, authentication, and account management
2. **Auth Sessions** — Refresh token-based session management with rotation and revocation
3. **Providers** — Catalog of AI providers (seeded via Flyway migration)
4. **Models** — AI models available from each provider with context window info
5. **Provider Configs** — Per-user provider credentials and endpoint settings
6. **Conversations** — User chat sessions linked to a model and provider config
7. **Messages** — Individual messages within conversations (USER/ASSISTANT roles)

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

#### CORS (Frontend URL)

The allowed frontend origin is configured in `application.yml`:

```yaml
app:
  cors:
    allowed-origins: http://localhost:5173
```

To change the frontend URL, update `app.cors.allowed-origins`. Multiple origins can be specified as a comma-separated list (e.g., `http://localhost:5173,http://localhost:3000`). No code changes are needed.

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
│   ├── ProviderController.java       # Provider & model read endpoints
│   └── UserController.java           # Signup, login, /users/me
├── dto/                              # Data transfer objects
│   ├── CreateConversationRequest.java
│   ├── CreateProviderConfigRequest.java
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
│   ├── AuthSession.java
│   ├── Conversation.java
│   ├── Message.java
│   ├── Model.java
│   ├── Provider.java
│   ├── ProviderConfig.java
│   └── User.java
├── repository/                       # Spring Data JPA repositories
│   ├── AuthSessionRepository.java
│   ├── ConversationRepository.java
│   ├── MessageRepository.java
│   ├── ModelRepository.java
│   ├── ProviderConfigRepository.java
│   ├── ProviderRepository.java
│   └── UserRepository.java
├── service/                          # Service interfaces
│   ├── AuthService.java
│   ├── AuthSessionService.java
│   ├── ConversationService.java
│   ├── JwtService.java
│   ├── MessageService.java
│   ├── ModelService.java
│   ├── ProviderApiService.java       # Provider API chat interface
│   ├── ProviderConfigService.java
│   ├── ProviderService.java
│   └── UserService.java
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
| `GET` | `/api/providers/{id}/models` | ✗ | Models for a provider |
| `GET` | `/api/models` | ✗ | List all active models |
| `GET` | `/api/models/{id}` | ✗ | Model details |
| `POST` | `/api/auth/logout-all` | ✓ | Revoke all sessions |
| `GET` | `/api/users/me` | ✓ | Get current user profile |
| `PATCH` | `/api/users/me` | ✓ | Update display name |
| `DELETE` | `/api/users/me` | ✓ | Delete account |
| `POST` | `/api/provider-configs` | ✓ | Add provider config with API key |
| `GET` | `/api/provider-configs` | ✓ | List user's configs |
| `GET` | `/api/provider-configs/{id}` | ✓ | Get one config (no secret) |
| `PATCH` | `/api/provider-configs/{id}` | ✓ | Update config |
| `DELETE` | `/api/provider-configs/{id}` | ✓ | Delete config |
| `POST` | `/api/provider-configs/{id}/test` | ✓ | Test connectivity |
| `POST` | `/api/conversations` | ✓ | Start a conversation |
| `GET` | `/api/conversations` | ✓ | List conversations (paginated) |
| `GET` | `/api/conversations/{id}` | ✓ | Conversation details |
| `PATCH` | `/api/conversations/{id}` | ✓ | Rename or change model/config |
| `DELETE` | `/api/conversations/{id}` | ✓ | Delete conversation |
| `GET` | `/api/conversations/{id}/messages` | ✓ | Message history |
| `POST` | `/api/conversations/{id}/messages` | ✓ | Send a message and get AI response |

## Authentication Flow

1. **Signup**: `POST /api/auth/signup` with email, display name, and password
2. **Login**: `POST /api/auth/login` with email and password → receives JWT access token + refresh token
3. **Authenticated Requests**: Include `Authorization: Bearer <token>` header
4. **Token Refresh**: `POST /api/auth/refresh` with refresh token when access token expires (24h)
5. **Logout**: `POST /api/auth/logout` to revoke current session or `POST /api/auth/logout-all` to revoke all

## Seeded Data

The database is pre-seeded with 5 providers and 17 models via Flyway migration `V2__seed_providers_and_models.sql`:

| Provider | Type | Models |
|----------|------|--------|
| OpenAI | Cloud | gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo |
| Anthropic | Cloud | Claude Sonnet 4, Claude Sonnet 4 (Thinking), Claude Opus 4, Claude Haiku 4 |
| Google AI | Cloud | Gemini 2.5 Pro, Gemini 2.5 Flash, Gemini 2.5 Flash Lite |
| Mistral AI | Cloud | Mistral Large, Mistral Small, Codestral |
| Ollama | Self-hosted | Llama 3.3, Mistral (Local), Phi 3 |

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

**Supported providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama.
Each provider has its own implementation class handling API-specific request/response formats.
Custom `endpointUrl` in provider configs overrides the default base URL, enabling proxy/self-hosted setups.

## Future Work

- Streaming responses for long assistant replies
- Message editing and regeneration
- Conversation sharing
- Admin endpoints for provider/model catalog management
- Usage analytics and rate limiting
- API credential encryption at rest
- Support for additional AI providers
