# Personal AI Backend - Agent Guidelines

## Project Overview
Spring Boot 4.1.2 backend for a personal AI chat application supporting multiple AI providers (OpenAI, Anthropic, Google AI, Mistral AI, Ollama) with JWT authentication, refresh token rotation, conversations, and message history.

## Tech Stack
- **Java 21** with **Spring Boot 4.1.2**
- **PostgreSQL 18+** with **Flyway** migrations
- **JPA/Hibernate 6** with **Lombok**
- **Spring Security** with **JJWT 0.12.6** (JWT access + refresh tokens)
- **BCrypt** for password hashing

## Configuration

### CORS (Cross-Origin Resource Sharing)
The frontend URL is configured in `src/main/resources/application.yml`:

```yaml
app:
  cors:
    allowed-origins: http://localhost:5173
```

To change the frontend URL, update `app.cors.allowed-origins` in `application.yml`. Multiple origins can be specified as a comma-separated list (e.g., `http://localhost:5173,http://localhost:3000`). No code changes are required.

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

## API Endpoints (28 total)

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
| `GET` | `/api/providers/{id}/models` | Active models for a provider |
| `GET` | `/api/models` | List all active models (optional `?providerId=`) |
| `GET` | `/api/models/{id}` | Model details including context window |

### Authenticated Endpoints (JWT Bearer Token Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/logout-all` | Revoke all user sessions |
| `GET` | `/api/users/me` | Get current user profile |
| `PATCH` | `/api/users/me` | Update display name |
| `DELETE` | `/api/users/me` | Delete account |
| `POST` | `/api/provider-configs` | Add provider config with API key |
| `GET` | `/api/provider-configs` | List user's configs (optional `?providerId=`) |
| `GET` | `/api/provider-configs/{id}` | Get one config (no secret exposed) |
| `PATCH` | `/api/provider-configs/{id}` | Update config name, endpoint, status, or credential |
| `DELETE` | `/api/provider-configs/{id}` | Delete config (if no conversation references it) |
| `POST` | `/api/provider-configs/{id}/test` | Test config connectivity |
| `POST` | `/api/conversations` | Start a conversation |
| `GET` | `/api/conversations` | List conversations (paginated, optional `?search=`) |
| `GET` | `/api/conversations/{id}` | Conversation details |
| `PATCH` | `/api/conversations/{id}` | Rename or change model/config |
| `DELETE` | `/api/conversations/{id}` | Delete conversation and messages |
| `GET` | `/api/conversations/{id}/messages` | Message history (paginated) |
| `POST` | `/api/conversations/{id}/messages` | Send user message, call provider API, get assistant reply |

### Error Page
- `GET /error` — Spring Boot default error handler (public, returns structured JSON errors)

## Authentication Flow

1. **Signup**: `POST /api/auth/signup` with email, displayName, password → user created with BCrypt-hashed password
2. **Login**: `POST /api/auth/login` with email, password → returns `token` (JWT access, 24h) + `refreshToken` (7h, stored as hash in AuthSession)
3. **Protected Requests**: Include `Authorization: Bearer <token>` header; `JwtFilter` validates and sets `SecurityContextHolder`
4. **Refresh**: `POST /api/auth/refresh` with refreshToken → rotates refresh token and issues new access token
5. **Logout**: `POST /api/auth/logout` with refreshToken → revokes current session; `POST /api/auth/logout-all` → revokes all sessions

## Seeded Data

Database is pre-seeded via `V2__seed_providers_and_models.sql`:
- **5 Providers**: OpenAI, Anthropic, Google AI, Mistral AI, Ollama
- **17 Models**: gpt-4o, gpt-4o-mini, gpt-4-turbo, gpt-3.5-turbo, Claude Sonnet 4, Claude Opus 4, Claude Haiku 4, Gemini 2.5 Pro, Gemini 2.5 Flash, Mistral Large, Codestral, Llama 3.3, Phi 3, etc.

## Project Structure

```
src/main/java/com/personal_ai/backend/
├── BackendApplication.java
├── config/
│   ├── SecurityConfig.java      # Security, CORS, BCrypt bean
│   └── JwtFilter.java           # OncePerRequestFilter for Bearer tokens
├── controller/
│   ├── AuthController.java      # /auth/refresh, /auth/logout, /auth/logout-all
│   ├── ConversationController.java  # /conversations CRUD + /messages
│   ├── HealthController.java    # /api/health
│   ├── ProviderConfigController.java  # /provider-configs CRUD + /test
│   ├── ProviderController.java  # /providers, /models (read-only)
│   └── UserController.java      # /auth/signup, /auth/login, /users/me
├── dto/                         # 11 DTOs for requests/responses
├── model/                       # 7 JPA entities (User, AuthSession, Provider, Model, ProviderConfig, Conversation, Message)
├── repository/                  # 7 Spring Data JPA repositories
├── service/                     # 10 service interfaces
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

**Supported providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama.
Each provider has its own implementation class handling API-specific request/response formats.
Custom `endpointUrl` in provider configs overrides the default base URL, enabling proxy/self-hosted setups.

## Known Limitations

- **Credential Encryption**: Provider config API keys are stored as plain byte arrays; encryption at rest is planned.
- **Streaming**: No streaming responses; send-message returns a completed response.
- **Provider API Timeouts**: Long-running model responses may hit the 2-minute timeout (5 minutes for Ollama).
