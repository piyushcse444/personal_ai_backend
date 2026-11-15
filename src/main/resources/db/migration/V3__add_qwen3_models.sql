-- Add Qwen3 models to Ollama provider

INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, created_at, updated_at)
VALUES
  (
    '18f9a0b1-c2d3-4567-fabc-678901234567',
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'qwen3:8b',
    'Qwen3 8B',
    131072,
    true,
    NOW(),
    NOW()
  ),
  (
    '19a0b1c2-d3e4-5678-abcd-789012345678',
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'qwen3:32b',
    'Qwen3 32B',
    131072,
    true,
    NOW(),
    NOW()
  ),
  (
    '20b1c2d3-e4f5-6789-bcde-890123456789',
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'qwen3:0.5b',
    'Qwen3 0.5B',
    131072,
    true,
    NOW(),
    NOW()
  );
