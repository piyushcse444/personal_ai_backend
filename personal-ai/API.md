# Personal AI Backend - API Documentation

Base URL: `http://localhost:8080`

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

### 8. `GET /api/providers/{id}/models`

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

### 9. `GET /api/models`

List all active models, optionally filtered by provider.

**Query Params:**
- `providerId` (optional) — filter by provider UUID

**Success Response (200 OK):** Array of model objects.

---

### 10. `GET /api/models/{id}`

Get details for a specific model.

**Success Response (200 OK):** Model object with contextWindow info.

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

Add a provider config with an API key.

**Request Body:**
```json
{
  "providerId": "550e8400-e29b-41d4-a716-446655440000",
  "configName": "My OpenAI Config",
  "apiKey": "sk-abc123...",
  "endpointUrl": "https://api.openai.com/v1"
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

Start a new conversation with a model and provider config.

**Request Body:**
```json
{
  "modelId": "model-uuid",
  "providerConfigId": "config-uuid",
  "title": "My Conversation"
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

Delete a conversation and its messages.

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Conversation deleted successfully"
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

The server fetches the conversation history, calls the provider's API using the conversation's model and config, saves both messages, and returns them.

**Supported Providers**: OpenAI, Anthropic, Google AI (Gemini), Mistral AI, Ollama.

**Request Body:**
```json
{
  "content": "Explain what a database index is."
}
```

**Success Response (200 OK):**
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
| `app.cors.allowed-origins` | `http://localhost:5173` | Allowed frontend origins (comma-separated) |
| `app.jwt.secret` | *(placeholder)* | JWT signing secret (change in production) |
| `app.jwt.expiration-ms` | `86400000` | JWT access token lifetime in milliseconds (24 hours) |
| `app.jwt.refresh-expiration-ms` | `604800000` | Refresh token lifetime in milliseconds (7 days) |
