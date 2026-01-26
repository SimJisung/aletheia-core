-- V2: Create thought_fragments table
-- ====================================
-- This table stores user's thought fragments in an APPEND-ONLY manner.
-- The textRaw field is IMMUTABLE - no updates allowed.
-- Only soft-delete (setting deleted_at) is permitted.

CREATE TABLE thought_fragments (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign key to user (in MVP, this may be a single user)
    user_id UUID NOT NULL,

    -- The raw text of the thought - IMMUTABLE, never modify
    text_raw TEXT NOT NULL,

    -- Timestamp when the fragment was created
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Emotional valence: -1.0 (very negative) to +1.0 (very positive)
    mood_valence DOUBLE PRECISION NOT NULL,

    -- Arousal/activation level: 0.0 (calm) to 1.0 (excited)
    arousal DOUBLE PRECISION NOT NULL,

    -- Optional topic hint for categorization
    topic_hint VARCHAR(255),

    -- Vector embedding for semantic search (1536 dimensions for OpenAI)
    embedding vector(1536),

    -- Soft-delete timestamp (NULL means not deleted)
    deleted_at TIMESTAMPTZ,

    -- Constraints to ensure valid ranges
    CONSTRAINT chk_mood_valence CHECK (mood_valence BETWEEN -1.0 AND 1.0),
    CONSTRAINT chk_arousal CHECK (arousal BETWEEN 0.0 AND 1.0),
    CONSTRAINT chk_text_not_empty CHECK (LENGTH(TRIM(text_raw)) > 0)
);

-- Indexes for common queries
-- --------------------------

-- Index for fetching user's fragments by time (newest first)
CREATE INDEX idx_fragments_user_created
    ON thought_fragments(user_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- Index for vector similarity search using HNSW algorithm
-- HNSW provides fast approximate nearest neighbor search
CREATE INDEX idx_fragments_embedding
    ON thought_fragments
    USING hnsw (embedding vector_cosine_ops)
    WHERE deleted_at IS NULL AND embedding IS NOT NULL;

-- Comment on table for documentation
COMMENT ON TABLE thought_fragments IS 'Append-only storage for user thought fragments. Text is immutable.';
COMMENT ON COLUMN thought_fragments.text_raw IS 'Raw text of the thought. IMMUTABLE - never update this field.';
COMMENT ON COLUMN thought_fragments.mood_valence IS 'Emotional valence from -1.0 (negative) to +1.0 (positive)';
COMMENT ON COLUMN thought_fragments.arousal IS 'Activation level from 0.0 (calm) to 1.0 (excited)';
COMMENT ON COLUMN thought_fragments.embedding IS 'Vector embedding for semantic similarity search (1536 dimensions)';
