-- V8: Align embedding vector dimension with active model
-- =====================================================

-- Rebuild vector index to avoid type mismatch issues
DROP INDEX IF EXISTS idx_fragments_embedding;

-- If existing embeddings don't match the target dimension, drop them to avoid cast errors
UPDATE thought_fragments
SET embedding = NULL
WHERE embedding IS NOT NULL
  AND vector_dims(embedding) <> ${embedding_dimensions};

-- Match column dimension to configured embedding size
ALTER TABLE thought_fragments
    ALTER COLUMN embedding TYPE vector(${embedding_dimensions})
    USING embedding::vector(${embedding_dimensions});

-- Recreate vector similarity index
CREATE INDEX idx_fragments_embedding
    ON thought_fragments
    USING hnsw (embedding vector_cosine_ops)
    WHERE deleted_at IS NULL AND embedding IS NOT NULL;

-- Update column comment to remove fixed dimension reference
COMMENT ON COLUMN thought_fragments.embedding IS 'Vector embedding for semantic similarity search';
