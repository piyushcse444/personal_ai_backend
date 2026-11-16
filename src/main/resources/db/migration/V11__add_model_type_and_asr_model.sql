-- Add model_type column to models table to identify the purpose of each model
-- Supported values: CHAT, ASR, TTS, EMBEDDING

ALTER TABLE models ADD COLUMN model_type VARCHAR(30) NOT NULL DEFAULT 'CHAT';

-- Seed the Qwen3-ASR 0.6B model for audio transcription
-- Using the existing Unsloth provider (f6a7b8c9-d0e1-2345-fabc-456789012345)
INSERT INTO models (id, provider_id, model_name, display_name, context_window, is_active, is_offline, supports_vision, model_type, created_at, updated_at)
VALUES
  (
    '43e4f5a6-b7c8-9012-efab-123456789def',
    'f6a7b8c9-d0e1-2345-fabc-456789012345',
    'qwen3-asr-0.6b',
    'Qwen3-ASR 0.6B',
    4096,
    true,
    true,
    false,
    'ASR',
    NOW(),
    NOW()
  );
