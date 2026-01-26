-- V5: Create user settings table
-- ================================
-- Stores user-specific settings and learning parameters.

CREATE TABLE user_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User ID (unique per user)
    user_id UUID NOT NULL UNIQUE,

    -- Lambda (λ): Regret sensitivity weight
    -- Updated based on feedback
    -- Higher = more weight on avoiding regret
    lambda DOUBLE PRECISION NOT NULL DEFAULT 1.0,

    -- Regret prior: Base regret probability before evidence
    regret_prior DOUBLE PRECISION NOT NULL DEFAULT 0.2,

    -- Created timestamp
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Last update timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Valid ranges
    CONSTRAINT chk_lambda CHECK (lambda BETWEEN 0.1 AND 5.0),
    CONSTRAINT chk_regret_prior CHECK (regret_prior BETWEEN 0.0 AND 1.0)
);

-- Index for lookup by user
CREATE INDEX idx_user_settings_user ON user_settings(user_id);

-- Comments
COMMENT ON TABLE user_settings IS 'User-specific settings and learning parameters';
COMMENT ON COLUMN user_settings.lambda IS 'Regret sensitivity weight. Updated based on feedback.';
COMMENT ON COLUMN user_settings.regret_prior IS 'Base regret probability before evidence.';
