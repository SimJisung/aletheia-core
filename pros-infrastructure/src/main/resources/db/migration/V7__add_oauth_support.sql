-- Add OAuth support to users and create oauth_accounts table
-- ============================================================

-- Add avatar_url column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);

-- Make password_hash nullable for OAuth-only users
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Create oauth_accounts table for linked social accounts
CREATE TABLE IF NOT EXISTS oauth_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    name VARCHAR(100),
    avatar_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Each provider + provider_user_id combination must be unique
    CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id)
);

-- Indexes for oauth_accounts
CREATE INDEX IF NOT EXISTS idx_oauth_accounts_user_id ON oauth_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_accounts_provider ON oauth_accounts(provider);
CREATE INDEX IF NOT EXISTS idx_oauth_accounts_email ON oauth_accounts(email);

-- Comments
COMMENT ON TABLE oauth_accounts IS 'Linked OAuth social login accounts';
COMMENT ON COLUMN oauth_accounts.id IS 'OAuth account link unique identifier';
COMMENT ON COLUMN oauth_accounts.user_id IS 'Reference to the user who owns this OAuth link';
COMMENT ON COLUMN oauth_accounts.provider IS 'OAuth provider name (GOOGLE, GITHUB)';
COMMENT ON COLUMN oauth_accounts.provider_user_id IS 'User ID from the OAuth provider';
COMMENT ON COLUMN oauth_accounts.email IS 'Email from OAuth provider (may differ from user email)';
COMMENT ON COLUMN oauth_accounts.name IS 'Display name from OAuth provider';
COMMENT ON COLUMN oauth_accounts.avatar_url IS 'Profile picture URL from OAuth provider';
COMMENT ON COLUMN oauth_accounts.created_at IS 'When the OAuth link was created';
COMMENT ON COLUMN users.avatar_url IS 'User profile picture URL';
