-- V3: Create value graph tables (value_nodes and value_edges)
-- ============================================================
-- These tables store the user's value graph.
-- value_nodes: One per user per value axis (8 total per user)
-- value_edges: Relationships between value axes (SUPPORT or CONFLICT)
--
-- IMPORTANT: Conflict edges are NEVER deleted. Contradictions are preserved.

-- Value nodes table
-- -----------------
CREATE TABLE value_nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign key to user
    user_id UUID NOT NULL,

    -- Value axis (one of 8 fixed values)
    axis VARCHAR(50) NOT NULL,

    -- Average emotional valence toward this value
    avg_valence DOUBLE PRECISION NOT NULL DEFAULT 0.0,

    -- Recent trend direction
    recent_trend VARCHAR(20) NOT NULL DEFAULT 'NEUTRAL',

    -- Count of fragments associated with this value
    fragment_count INT NOT NULL DEFAULT 0,

    -- Last update timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Each user has exactly one node per axis
    CONSTRAINT uq_value_node_user_axis UNIQUE (user_id, axis),

    -- Valid axis values
    CONSTRAINT chk_valid_axis CHECK (axis IN (
        'GROWTH', 'STABILITY', 'FINANCIAL', 'AUTONOMY',
        'RELATIONSHIP', 'ACHIEVEMENT', 'HEALTH', 'MEANING'
    )),

    -- Valid trend values
    CONSTRAINT chk_valid_trend CHECK (recent_trend IN ('RISING', 'FALLING', 'NEUTRAL')),

    -- Valid ranges
    CONSTRAINT chk_avg_valence CHECK (avg_valence BETWEEN -1.0 AND 1.0),
    CONSTRAINT chk_fragment_count CHECK (fragment_count >= 0)
);

-- Index for fetching all nodes for a user
CREATE INDEX idx_value_nodes_user ON value_nodes(user_id);

-- Value edges table
-- -----------------
CREATE TABLE value_edges (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Foreign key to user
    user_id UUID NOT NULL,

    -- Source value axis
    from_axis VARCHAR(50) NOT NULL,

    -- Target value axis
    to_axis VARCHAR(50) NOT NULL,

    -- Edge type: SUPPORT or CONFLICT
    -- IMPORTANT: CONFLICT edges are NEVER deleted
    edge_type VARCHAR(20) NOT NULL,

    -- Weight of the relationship (0.0 to 1.0)
    weight DOUBLE PRECISION NOT NULL DEFAULT 0.0,

    -- Last update timestamp
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Unique constraint per user and axis pair
    CONSTRAINT uq_value_edge_user_axes UNIQUE (user_id, from_axis, to_axis),

    -- Prevent self-loops
    CONSTRAINT chk_no_self_loop CHECK (from_axis != to_axis),

    -- Valid axis values
    CONSTRAINT chk_valid_from_axis CHECK (from_axis IN (
        'GROWTH', 'STABILITY', 'FINANCIAL', 'AUTONOMY',
        'RELATIONSHIP', 'ACHIEVEMENT', 'HEALTH', 'MEANING'
    )),
    CONSTRAINT chk_valid_to_axis CHECK (to_axis IN (
        'GROWTH', 'STABILITY', 'FINANCIAL', 'AUTONOMY',
        'RELATIONSHIP', 'ACHIEVEMENT', 'HEALTH', 'MEANING'
    )),

    -- Valid edge types
    CONSTRAINT chk_valid_edge_type CHECK (edge_type IN ('SUPPORT', 'CONFLICT')),

    -- Valid weight range
    CONSTRAINT chk_weight CHECK (weight BETWEEN 0.0 AND 1.0)
);

-- Index for fetching all edges for a user
CREATE INDEX idx_value_edges_user ON value_edges(user_id);

-- Index for finding edges connected to a specific axis
CREATE INDEX idx_value_edges_from ON value_edges(user_id, from_axis);
CREATE INDEX idx_value_edges_to ON value_edges(user_id, to_axis);

-- Comments
COMMENT ON TABLE value_nodes IS 'Stores user value axis statistics. One node per user per axis.';
COMMENT ON TABLE value_edges IS 'Stores relationships between value axes. CONFLICT edges are NEVER deleted.';
COMMENT ON COLUMN value_edges.edge_type IS 'SUPPORT or CONFLICT. Conflicts are preserved, not resolved.';
