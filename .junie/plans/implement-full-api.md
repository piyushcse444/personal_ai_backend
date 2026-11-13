---
sessionId: session-261101-131322-11n9
---

# Current State & Requirements

## Current State

The project has a **complete service layer** but **no REST controllers** (except `UserController` with signup/login and `HealthController`). All 7 entities, repositories, and service implementations are fully functional.

### What exists
- **Entities**: User, AuthSession, Provider, Model, ProviderConfig, Conversation, Message
- **Services**: UserService, AuthService, AuthSessionService, ProviderService, ModelService, ProviderConfigService, ConversationService, MessageService, JwtService
- **Controllers**: UserController (signup/login), HealthController
- **Auth**: JWT access token (24h), BCrypt password hashing, CORS configured

### What's missing
- Refresh token flow (refresh/logout/logout-all)
- `/users/me` (GET, PATCH, DELETE)
- Provider & Model read-only endpoints
- ProviderConfig CRUD with ownership enforcement
- Conversation CRUD with ownership enforcement
- Message history + send message endpoint
- DTOs for all new endpoints

## Requirements (from the spec)

### Authentication & Account
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /auth/refresh | Rotate refresh token, issue new access token |
| POST | /auth/logout | Revoke current session |
| POST | /auth/logout-all | Revoke all sessions |
| GET | /users/me | Get current user profile |
| PATCH | /users/me | Update display name |
| DELETE | /users/me | Delete account |

### Providers & Models (read-only)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /providers | List active providers |
| GET | /providers/{id} | Provider details |
| GET | /providers/{id}/models | Active models for a provider |
| GET | /models | List active models, optional ?providerId= filter |
| GET | /models/{id} | Model details |

### Provider Configs (user-owned)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /provider-configs | Add config with API key |
| GET | /provider-configs | List user's configs, optional ?providerId= |
| GET | /provider-configs/{id} | Get one config (no secret) |
| PATCH | /provider-configs/{id} | Update name, endpoint, status, or replace credential |
| DELETE | /provider-configs/{id} | Delete if no conversation references it |
| POST | /provider-configs/{id}/test | Test connectivity |

### Conversations (user-owned)
| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /conversations | Start with modelId, providerConfigId, title |
| GET | /conversations | List with pagination & title search |
| GET | /conversations/{id} | Details |
| PATCH | /conversations/{id} | Rename or change model/config |
| DELETE | /conversations/{id} | Delete and messages |

### Messages
| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /conversations/{id}/messages | History, paginated |
| POST | /conversations/{id}/messages | Send user message, get assistant reply |

## Build Order
1. Refresh token + logout + logout-all
2. GET/PATCH/DELETE /users/me
3. Provider & Model read endpoints
4. ProviderConfig CRUD
5. Conversation CRUD
6. Message history + send message
7. API.md documentation update

# Technical Design

## Key Decisions

### 1. Refresh Token Strategy
The `AuthSession` entity already exists with `refreshTokenHash`, `expiresAt`, `revokedAt`, and `replacedBy` fields. The plan uses **refresh tokens stored in the database** — the refresh token is returned as a plain string in the login/refresh response, and the client sends it back in the request body.

**Why**: The AuthSession entity is already designed for this pattern. Storing the hash in DB allows logout/revoke to work by marking sessions as revoked.

### 2. User ID Extraction
The current `JwtFilter` stores the user ID as a `String` in `SecurityContextHolder`. A helper method `getCurrentUserId()` will be used in all controllers to extract it consistently.

### 3. Ownership Enforcement
Every endpoint that accesses a Conversation, ProviderConfig, or Message will verify that the resource belongs to the authenticated user. This is done in the **service layer** by adding ownership-aware methods.

### 4. Provider Config Credential Handling
- **Create**: Accept `apiKey` (plain text) in the DTO, store as `credentialCiphertext` (plain text byte array for now; encryption can be added later).
- **Read**: Never return the credential. Return `credentialConfigured: true/false` and `credentialMasked: "****last4chars"`.
- **Update**: Accept optional `apiKey` to replace; if not provided, keep existing.

### 5. Send Message (POST /conversations/{id}/messages)
This endpoint now fully implements the provider API call:
1. Verify conversation ownership
2. Save the USER message
3. **Call the selected provider's API** using the conversation's model and config
4. Save and return the ASSISTANT response with token counts

**Supported providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama.
Each provider has its own implementation class handling API-specific request/response formats.

### 6. Pagination
Using Spring Data's `Pageable` for `/conversations` and `/conversations/{id}/messages` with `?page=0&size=20` query params.

### 7. Controller Organization
- **AuthController.java** (new) — POST /auth/refresh, /auth/logout, /auth/logout-all
- **UserController.java** (modified) — add GET/PATCH/DELETE /users/me (keeps signup/login)
- **ProviderController.java** (new) — read-only provider & model endpoints
- **ProviderConfigController.java** (new) — user's provider configs CRUD + test
- **ConversationController.java** (new) — conversations CRUD + messages

```
graph LR
    A[AuthController] --> B[AuthService]
    A --> C[AuthSessionService]
    D[UserController] --> E[UserService]
    F[ProviderController] --> G[ProviderService]
    F --> H[ModelService]
    I[ProviderConfigController] --> J[ProviderConfigService]
    K[ConversationController] --> L[ConversationService]
    K --> M[MessageService]
```

### Proposed Changes

#### New DTOs (10 files)
- `RefreshRequest.java` — refreshToken
- `CreateProviderConfigRequest.java` — providerId, configName, apiKey, endpointUrl
- `UpdateProviderConfigRequest.java` — configName, apiKey, endpointUrl, isActive
- `ProviderConfigResponse.java` — id, configName, provider info, credentialConfigured, credentialMasked, isActive
- `CreateConversationRequest.java` — modelId, providerConfigId, title
- `UpdateConversationRequest.java` — title, modelId, providerConfigId
- `ConversationResponse.java` — id, title, model info, updatedAt
- `SendMessageRequest.java` — content
- `MessageResponse.java` — id, role, content, tokenCount, createdAt
- `UpdateUserRequest.java` — displayName

#### New/Modified Controllers (5 files)
- **AuthController.java** (new) — POST /api/auth/refresh, /api/auth/logout, /api/auth/logout-all
- **UserController.java** (modified) — add GET/PATCH/DELETE /api/users/me
- **ProviderController.java** (new) — GET /api/providers, /providers/{id}, /providers/{id}/models, /models, /models/{id}
- **ProviderConfigController.java** (new) — CRUD + test for provider configs
- **ConversationController.java** (new) — CRUD for conversations + messages

#### Modified Services (3 files)
- **AuthService.java / AuthServiceImpl.java** — add refresh(), logout(), logoutAll()
- **ProviderConfigService.java / ProviderConfigServiceImpl.java** — add ownership-aware findById, testConfig
- **ConversationService.java / ConversationServiceImpl.java** — add ownership-aware findById, title search

#### Modified Config (1 file)
- **SecurityConfig.java** — permit /auth/refresh, /auth/logout, /auth/logout-all

#### Documentation (1 file)
- **API.md** — update with all new endpoints

### File Structure

```
dto/
  + RefreshRequest.java
  + CreateProviderConfigRequest.java
  + UpdateProviderConfigRequest.java
  + ProviderConfigResponse.java
  + CreateConversationRequest.java
  + UpdateConversationRequest.java
  + ConversationResponse.java
  + SendMessageRequest.java
  + MessageResponse.java
  + UpdateUserRequest.java

controller/
  + AuthController.java
  * UserController.java (add /users/me)
  + ProviderController.java
  + ProviderConfigController.java
  + ConversationController.java

service/
  * AuthService.java (add refresh, logout, logoutAll)
  * ProviderConfigService.java (add ownership methods)
  * ConversationService.java (add ownership + search)

service/impl/
  * AuthServiceImpl.java
  * ProviderConfigServiceImpl.java
  * ConversationServiceImpl.java

config/
  * SecurityConfig.java (permit new auth endpoints)

personal-ai/
  * API.md (update)
```

# Delivery Steps

### ✓ Step 1: Refresh token flow + logout endpoints
Implement POST /api/auth/refresh, /api/auth/logout, /api/auth/logout-all with refresh token storage in AuthSession.
- Add `generateRefreshToken(User)` to `JwtService.java`
- Add `refresh(refreshToken)`, `logout(refreshToken)`, `logoutAllByUserId(userId)` to `AuthService.java` and `AuthServiceImpl.java`
- Create `RefreshRequest.java` DTO with `refreshToken` field
- Create `AuthController.java` with 3 endpoints at `/api/auth/refresh`, `/api/auth/logout`, `/api/auth/logout-all`
- Update `SecurityConfig.java` to permit the new endpoints
- Update `AuthServiceImpl.login()` to also return a refresh token and create an AuthSession
- Login response now returns both `token` (access) and `refreshToken`

### ✓ Step 2: GET/PATCH/DELETE /users/me
Implement user profile endpoints for the authenticated user.
- Create `UpdateUserRequest.java` DTO with optional `displayName` field
- Add GET `/api/users/me` to `UserController.java` — returns user id, email, displayName
- Add PATCH `/api/users/me` — updates displayName via `UserService.updateUser()`
- Add DELETE `/api/users/me` — deletes account via `UserService.deleteUser()`
- Extract userId from `SecurityContextHolder` in each endpoint

### ✓ Step 3: Provider & Model read-only endpoints
Implement read-only catalog endpoints for providers and models.
- Create `ProviderController.java` with:
  - GET `/api/providers` → list active providers via `ProviderService.findActiveProviders()`
  - GET `/api/providers/{id}` → provider details with models
  - GET `/api/providers/{id}/models` → active models for a provider
  - GET `/api/models` → list all active models, optional `?providerId=` filter
  - GET `/api/models/{id}` → model details including contextWindow
- Use existing `ProviderService` and `ModelService` implementations (no service changes needed)

### ✓ Step 4: ProviderConfig CRUD with ownership
Implement user's provider config management with credential masking.
- Create DTOs: `CreateProviderConfigRequest`, `UpdateProviderConfigRequest`, `ProviderConfigResponse`
- Add `findByUserIdAndId(userId, configId)` to `ProviderConfigService`/Impl for ownership checks
- Create `ProviderConfigController.java` with:
  - POST `/api/provider-configs` — create with apiKey, extract userId from JWT
  - GET `/api/provider-configs` — list user's configs, optional `?providerId=`
  - GET `/api/provider-configs/{id}` — single config, no secret exposed
  - PATCH `/api/provider-configs/{id}` — update fields, verify ownership
  - DELETE `/api/provider-configs/{id}` — verify no conversation references it
  - POST `/api/provider-configs/{id}/test` — placeholder returning success (actual API call TBD)
- Response DTO masks credentials: `credentialConfigured` + `credentialMasked`

### ✓ Step 5: Conversation CRUD with ownership
Implement conversation management with ownership enforcement and pagination.
- Create DTOs: `CreateConversationRequest`, `UpdateConversationRequest`, `ConversationResponse`
- Add `findByUserIdAndId(userId, conversationId)` to `ConversationService`/Impl for ownership checks
- Add title search query method to `ConversationRepository`
- Create `ConversationController.java` with:
  - POST `/api/conversations` — create with modelId, providerConfigId, title; validate config belongs to user
  - GET `/api/conversations` — paginated list with optional `?search=` title filter
  - GET `/api/conversations/{id}` — details, ownership check
  - PATCH `/api/conversations/{id}` — rename or change model/config together
  - DELETE `/api/conversations/{id}` — delete conversation and cascade messages

### ✓ Step 6: Message history + send message + API docs
Implement message loading, sending, and update API documentation.
- Create DTOs: `SendMessageRequest`, `MessageResponse`
- Add message endpoints to `ConversationController.java`:
  - GET `/api/conversations/{id}/messages` — paginated history in chronological order
  - POST `/api/conversations/{id}/messages` — save USER message, call provider API, save ASSISTANT response, return both
- Verify conversation ownership before any message operation
- **Provider API integration completed**: 5 provider implementations (OpenAI, Anthropic, Google AI, Mistral AI, Ollama) with factory pattern
- Update `API.md` with all new endpoints, request/response examples, and complete auth flow