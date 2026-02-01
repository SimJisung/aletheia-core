-- V9: Create value importance table
-- ================================
-- Stores user's explicit value importance ratings.

CREATE TABLE value_importance (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- User reference
    user_id UUID NOT NULL,

    -- Importance values for each axis (stored as JSONB for flexibility)
    importance_map JSONB NOT NULL DEFAULT '{}',

    -- Version for change tracking
    version INTEGER NOT NULL DEFAULT 1,

    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_version_positive CHECK (version >= 1)
);

-- Unique constraint: one active record per user
CREATE UNIQUE INDEX idx_value_importance_user ON value_importance(user_id);

-- Index for version history queries
CREATE INDEX idx_value_importance_user_version ON value_importance(user_id, version);

-- Comments
COMMENT ON TABLE value_importance IS 'User explicit value axis importance ratings';
COMMENT ON COLUMN value_importance.importance_map IS 'Map of ValueAxis name to normalized importance (0.0-1.0)';
COMMENT ON COLUMN value_importance.version IS 'Version number, increments on each update';
