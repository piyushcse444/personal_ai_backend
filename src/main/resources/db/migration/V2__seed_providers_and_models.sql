-- Seed providers table with popular AI providers

INSERT INTO providers (id, code, display_name, provider_type, base_url, is_active, created_at, updated_at)
VALUES
  (
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'openai',
    'OpenAI',
    'CLOUD',
    'https://api.openai.com',
    true,
    NOW(),
    NOW()
  ),
  (
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'anthropic',
    'Anthropic',
    'CLOUD',
    'https://api.anthropic.com',
    true,
    NOW(),
    NOW()
  ),
  (
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'google',
    'Google AI',
    'CLOUD',
    'https://generativelanguage.googleapis.com',
    true,
    NOW(),
    NOW()
  ),
  (
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'mistral',
    'Mistral AI',
    'CLOUD',
    'https://api.mistral.ai',
    true,
    NOW(),
    NOW()
  ),
  (
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'ollama',
    'Ollama',
    'SELF_HOSTED',
    NULL,
    true,
    NOW(),
    NOW()
  );

-- Seed models table with popular models for each provider

-- OpenAI models
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, created_at, updated_at)
VALUES
  (
    '11a2b3c4-d5e6-7890-abcd-ef1234567890',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'gpt-4o',
    'GPT-4o',
    128000,
    true,
    NOW(),
    NOW()
  ),
  (
    '12b3c4d5-e6f7-8901-bcde-f12345678901',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'gpt-4o-mini',
    'GPT-4o Mini',
    128000,
    true,
    NOW(),
    NOW()
  ),
  (
    '13c4d5e6-f7a8-9012-cdef-123456789012',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'gpt-4-turbo',
    'GPT-4 Turbo',
    128000,
    true,
    NOW(),
    NOW()
  ),
  (
    '14d5e6f7-a8b9-0123-defa-234567890123',
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'gpt-3.5-turbo',
    'GPT-3.5 Turbo',
    16385,
    true,
    NOW(),
    NOW()
  );

-- Anthropic models
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, created_at, updated_at)
VALUES
  (
    '15e6f7a8-b9c0-1234-efab-345678901234',
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'claude-sonnet-4-20250514',
    'Claude Sonnet 4',
    200000,
    true,
    NOW(),
    NOW()
  ),
  (
    '16f7a8b9-c0d1-2345-fabc-456789012345',
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'claude-sonnet-4-20250514-thinking',
    'Claude Sonnet 4 (Thinking)',
    200000,
    true,
    NOW(),
    NOW()
  ),
  (
    '17a8b9c0-d1e2-3456-abcd-567890123456',
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'claude-opus-4-20250514',
    'Claude Opus 4',
    200000,
    true,
    NOW(),
    NOW()
  ),
  (
    '18b9c0d1-e2f3-4567-bcde-678901234567',
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'claude-haiku-4-20250514',
    'Claude Haiku 4',
    200000,
    true,
    NOW(),
    NOW()
  );

-- Google AI models
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, created_at, updated_at)
VALUES
  (
    '19c0d1e2-f3a4-5678-cdef-789012345678',
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'gemini-2.5-pro',
    'Gemini 2.5 Pro',
    1048576,
    true,
    NOW(),
    NOW()
  ),
  (
    '10d1e2f3-a4b5-6789-defa-890123456789',
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'gemini-2.5-flash',
    'Gemini 2.5 Flash',
    1048576,
    true,
    NOW(),
    NOW()
  ),
  (
    '11e2f3a4-b5c6-7890-efab-901234567890',
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'gemini-2.5-flash-lite',
    'Gemini 2.5 Flash Lite',
    1048576,
    true,
    NOW(),
    NOW()
  );

-- Mistral AI models
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, created_at, updated_at)
VALUES
  (
    '12f3a4b5-c6d7-8901-fabc-012345678901',
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'mistral-large-latest',
    'Mistral Large',
    131072,
    true,
    NOW(),
    NOW()
  ),
  (
    '13a4b5c6-d7e8-9012-abcd-123456789012',
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'mistral-small-latest',
    'Mistral Small',
    32768,
    true,
    NOW(),
    NOW()
  ),
  (
    '14b5c6d7-e8f9-0123-bcde-234567890123',
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'codestral-latest',
    'Codestral',
    256000,
    true,
    NOW(),
    NOW()
  );

-- Ollama models (local, self-hosted)
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, created_at, updated_at)
VALUES
  (
    '15c6d7e8-f9a0-1234-cdef-345678901234',
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'llama3.3',
    'Llama 3.3',
    131072,
    true,
    NOW(),
    NOW()
  ),
  (
    '16d7e8f9-a0b1-2345-defa-456789012345',
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'mistral',
    'Mistral (Local)',
    32768,
    true,
    NOW(),
    NOW()
  ),
  (
    '17e8f9a0-b1c2-3456-efab-567890123456',
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'phi3',
    'Phi 3',
    128000,
    true,
    NOW(),
    NOW()
  );
