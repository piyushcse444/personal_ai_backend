-- Make provider_config_id nullable to support offline models (e.g., Llama.cpp)
-- that do not require a provider config
ALTER TABLE conversations ALTER COLUMN provider_config_id DROP NOT NULL;
