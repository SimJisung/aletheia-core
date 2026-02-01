-- V10: Add explanation columns to decisions table
-- ==============================================
-- Stores LLM-generated explanations to avoid repeated API calls.
-- Explanations are generated on first request and cached.

ALTER TABLE decisions
    ADD COLUMN explanation_summary TEXT,
    ADD COLUMN explanation_evidence_summary TEXT,
    ADD COLUMN explanation_value_summary TEXT,
    ADD COLUMN explanation_generated_at TIMESTAMPTZ;

-- Index for finding decisions without explanations (for potential background generation)
CREATE INDEX idx_decisions_no_explanation ON decisions(user_id, created_at DESC)
    WHERE explanation_summary IS NULL;

-- Comments
COMMENT ON COLUMN decisions.explanation_summary IS 'LLM-generated summary explaining calculation results';
COMMENT ON COLUMN decisions.explanation_evidence_summary IS 'LLM-generated summary of evidence fragments';
COMMENT ON COLUMN decisions.explanation_value_summary IS 'LLM-generated summary of value considerations';
COMMENT ON COLUMN decisions.explanation_generated_at IS 'Timestamp when explanation was generated';
