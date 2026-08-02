-- V011__ensure_audit_log_table.sql
--
-- Safety net: guarantees the audit_logs table exists with the V008
-- schema (company_id, module, jsonb, etc.).
--
-- Scenarios handled:
--   1. Clean build after V008 fix -> V008 already created the table, no-op.
--   2. Existing production (V008 already ran)   -> no-op.
--   3. Broken state (V001 ran, V008 never ran)  -> recreates with V008 schema.
--   4. Missing table entirely                    -> creates from scratch.
--

DO $$
BEGIN
    ------------------------------------------------------------------
    -- 1. Check whether the table already has the target schema
    --    (presence of the 'company_id' column = V008 marker)
    ------------------------------------------------------------------
    IF EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_name = 'audit_logs'
    ) AND EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_name = 'audit_logs' AND column_name = 'company_id'
    ) THEN
        -- Already a V008 table ÔÇô nothing to do
        RETURN;
    END IF;

    ------------------------------------------------------------------
    -- 2. Drop old table if present (V001 schema or any other shape)
    ------------------------------------------------------------------
    DROP TABLE IF EXISTS audit_logs CASCADE;

    ------------------------------------------------------------------
    -- 3. Create with V008 schema
    ------------------------------------------------------------------
    CREATE TABLE audit_logs (
        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        company_id UUID NOT NULL,
        user_id UUID,
        user_name VARCHAR(255),
        user_email VARCHAR(255),
        action VARCHAR(50) NOT NULL,
        module VARCHAR(50) NOT NULL,
        entity_name VARCHAR(100),
        entity_id VARCHAR(100),
        description TEXT,
        old_values JSONB,
        new_values JSONB,
        ip_address VARCHAR(45),
        user_agent TEXT,
        request_method VARCHAR(10),
        request_uri TEXT,
        status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
        success BOOLEAN NOT NULL DEFAULT TRUE,
        created_at TIMESTAMP NOT NULL DEFAULT NOW()
    );

    CREATE INDEX idx_audit_logs_company_id ON audit_logs(company_id);
    CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
    CREATE INDEX idx_audit_logs_action ON audit_logs(action);
    CREATE INDEX idx_audit_logs_module ON audit_logs(module);
    CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_name, entity_id);
    CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
    CREATE INDEX idx_audit_logs_status ON audit_logs(status);

END $$;

-- Seed audit permissions (safe to re-run ÔÇô ON CONFLICT is idempotent)
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('audit:read', 'View audit logs', 'audit', 'audit', 'read'),
    ('audit:export', 'Export audit logs', 'audit', 'audit', 'export')
ON CONFLICT (name) DO NOTHING;
