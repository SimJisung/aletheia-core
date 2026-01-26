-- V1: Enable required PostgreSQL extensions
-- ===========================================

-- Enable pgvector for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable uuid-ossp for UUID generation (optional, but useful)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
