-- Add supports_vision column to models table to indicate which models can process images

ALTER TABLE models ADD COLUMN supports_vision BOOLEAN NOT NULL DEFAULT false;

-- The Gemma-4-E4B model supports vision (image input), LFM3 does not
UPDATE models
SET supports_vision = true,
    updated_at = NOW()
WHERE model_name = 'HauhauCS/Gemma-4-E4B-Uncensored-HauhauCS-Aggressive';

UPDATE models
SET supports_vision = false,
    updated_at = NOW()
WHERE model_name = 'AtomicChat/LFM3-1.7B-Uncensored-GGUF';
