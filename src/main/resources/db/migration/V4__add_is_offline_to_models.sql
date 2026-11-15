-- Add is_offline column to models table and mark Ollama models as offline

ALTER TABLE models ADD COLUMN is_offline BOOLEAN NOT NULL DEFAULT false;

-- Mark all Ollama models as offline (they run locally and don't need an API key)
UPDATE models
SET is_offline = true
WHERE provider_id = 'e5f6a7b8-c9d0-1234-efab-345678901234';
