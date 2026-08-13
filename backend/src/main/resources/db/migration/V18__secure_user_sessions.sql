ALTER TABLE users ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiration ON refresh_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expiration ON revoked_tokens(expires_at);
