ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS has_password BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS user_providers (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id UUID NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user_providers_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_user_providers UNIQUE (provider, provider_id)
);

CREATE INDEX IF NOT EXISTS idx_user_providers_user_id ON user_providers(user_id);