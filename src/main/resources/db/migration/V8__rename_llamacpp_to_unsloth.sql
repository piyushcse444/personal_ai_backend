-- Rename llamacpp provider to unsloth and add Gemma model

-- Update provider code and display name
UPDATE providers
SET code = 'unsloth',
    display_name = 'Unsloth Studio',
    base_url = 'http://127.0.0.1:8888',
    updated_at = NOW()
WHERE code = 'llamacpp';

-- Add the second Unsloth model: Gemma-4-E4B
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, is_offline, created_at, updated_at)
VALUES
  (
    '32d3e4f5-a6b7-8901-defa-012345678abc',
    'f6a7b8c9-d0e1-2345-fabc-456789012345',
    'HauhauCS/Gemma-4-E4B-Uncensored-HauhauCS-Aggressive',
    'Gemma-4-E4B Uncensored (Offline)',
    20992,
    true,
    true,
    NOW(),
    NOW()
  );

-- Update context window for the existing LFM3 model to match config
UPDATE models
SET context_window = 20480,
    updated_at = NOW()
WHERE model_name = 'AtomicChat/LFM3-1.7B-Uncensored-GGUF';
