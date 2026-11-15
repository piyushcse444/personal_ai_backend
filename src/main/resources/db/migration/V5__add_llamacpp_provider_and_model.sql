-- Add llama.cpp local provider and offline model

INSERT INTO providers (id, code, display_name, provider_type, base_url, is_active, created_at, updated_at)
VALUES
  (
    'f6a7b8c9-d0e1-2345-fabc-456789012345',
    'llamacpp',
    'Llama.cpp (Local)',
    'LOCAL',
    'http://localhost:9901',
    true,
    NOW(),
    NOW()
  );

-- Insert the offline llama.cpp model with a large context window
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, is_offline, created_at, updated_at)
VALUES
  (
    '21c2d3e4-f5a6-7890-cdef-901234567890',
    'f6a7b8c9-d0e1-2345-fabc-456789012345',
    'AtomicChat/LFM3-1.7B-Uncensored-GGUF',
    'LFM3-1.7B Uncensored (Offline)',
    8192,
    true,
    true,
    NOW(),
    NOW()
  );
