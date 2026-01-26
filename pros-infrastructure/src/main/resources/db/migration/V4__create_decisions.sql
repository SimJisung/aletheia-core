-- V4: Create decisions and decision_feedbacks tables
-- ===================================================
-- These tables store decision projections and user feedback.
-- Decisions are binary (A/B) only in MVP.
-- Feedback is used to improve regret prediction.

-- Decisions table
-- ---------------
CREATE TABLE decisions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign key to user
    user_id UUID NOT NULL,

    -- Decision title/question
    title VARCHAR(500) NOT NULL,

    -- Option A description
    option_a TEXT NOT NULL,

    -- Option B description
    option_b TEXT NOT NULL,

    -- Optional priority value axis
    priority_axis VARCHAR(50),

    -- Computed probability for option A (0.0 to 1.0)
    probability_a DOUBLE PRECISION NOT NULL,

    -- Computed probability for option B (0.0 to 1.0)
    probability_b DOUBLE PRECISION NOT NULL,

    -- Computed regret risk for option A (0.0 to 1.0)
    regret_risk_a DOUBLE PRECISION NOT NULL,

    -- Computed regret risk for option B (0.0 to 1.0)
    regret_risk_b DOUBLE PRECISION NOT NULL,

    -- IDs of evidence fragments used for projection
    evidence_fragment_ids UUID[] NOT NULL DEFAULT '{}',

    -- Value alignment scores as JSONB
    value_alignment JSONB NOT NULL DEFAULT '{}',

    -- Timestamp when decision was created
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Valid priority axis
    CONSTRAINT chk_valid_priority_axis CHECK (priority_axis IS NULL OR priority_axis IN (
        'GROWTH', 'STABILITY', 'FINANCIAL', 'AUTONOMY',
        'RELATIONSHIP', 'ACHIEVEMENT', 'HEALTH', 'MEANING'
    )),

    -- Valid probability ranges
    CONSTRAINT chk_probability_a CHECK (probability_a BETWEEN 0.0 AND 1.0),
    CONSTRAINT chk_probability_b CHECK (probability_b BETWEEN 0.0 AND 1.0),

    -- Valid regret risk ranges
    CONSTRAINT chk_regret_risk_a CHECK (regret_risk_a BETWEEN 0.0 AND 1.0),
    CONSTRAINT chk_regret_risk_b CHECK (regret_risk_b BETWEEN 0.0 AND 1.0),

    -- Non-empty options
    CONSTRAINT chk_title_not_empty CHECK (LENGTH(TRIM(title)) > 0),
    CONSTRAINT chk_option_a_not_empty CHECK (LENGTH(TRIM(option_a)) > 0),
    CONSTRAINT chk_option_b_not_empty CHECK (LENGTH(TRIM(option_b)) > 0)
);

-- Index for fetching user's decisions by time
CREATE INDEX idx_decisions_user_created ON decisions(user_id, created_at DESC);

-- Decision feedbacks table
-- ------------------------
CREATE TABLE decision_feedbacks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign key to decision
    decision_id UUID NOT NULL REFERENCES decisions(id) ON DELETE CASCADE,

    -- Feedback type: SATISFIED, NEUTRAL, or REGRET
    feedback_type VARCHAR(20) NOT NULL,

    -- Timestamp when feedback was submitted
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- One feedback per decision
    CONSTRAINT uq_feedback_decision UNIQUE (decision_id),

    -- Valid feedback types
    CONSTRAINT chk_valid_feedback_type CHECK (feedback_type IN ('SATISFIED', 'NEUTRAL', 'REGRET'))
);

-- Index for finding feedback by decision
CREATE INDEX idx_feedbacks_decision ON decision_feedbacks(decision_id);

-- Comments
COMMENT ON TABLE decisions IS 'Stores decision projections. Binary A/B choices only.';
COMMENT ON TABLE decision_feedbacks IS 'Stores user feedback on decisions. Used to improve predictions.';
COMMENT ON COLUMN decisions.probability_a IS 'NOT a recommendation. Reflects pattern fit, not "better" option.';
COMMENT ON COLUMN decisions.probability_b IS 'NOT a recommendation. Reflects pattern fit, not "better" option.';
