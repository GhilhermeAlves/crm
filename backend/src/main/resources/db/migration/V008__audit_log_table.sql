-- V008__audit_log_table.sql
-- Create audit_logs table and seed audit permissions

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

-- Seed audit permissions
INSERT INTO permissions (name, description, module, resource, action) VALUES
    ('audit:read', 'View audit logs', 'audit', 'audit', 'read'),
    ('audit:export', 'Export audit logs', 'audit', 'audit', 'export')
ON CONFLICT (name) DO NOTHING;
