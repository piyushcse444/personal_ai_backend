CREATE TABLE archive_conversation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_email VARCHAR(255) NOT NULL,
    provider_name VARCHAR(255) NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    provider_config_name VARCHAR(255),
    archived_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE archive_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    archive_conversation_id UUID NOT NULL REFERENCES archive_conversation(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER,
    model_name VARCHAR(255),
    archived_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_archive_conversation_user_email ON archive_conversation(user_email);
CREATE INDEX idx_archive_message_conversation_id ON archive_message(archive_conversation_id);
