-- V013__multi_tenancy_infrastructure.sql
-- Multi-tenancy infrastructure: indexes, RLS, tenant context function

-- Schema para funções de multi-tenancy (app.current_tenant_id, app.is_super_admin)
CREATE SCHEMA IF NOT EXISTS app;

-- Add missing company_id index on user_roles for tenant isolation queries
CREATE INDEX IF NOT EXISTS idx_user_roles_company_id ON user_roles(company_id);

-- Function to get current tenant ID from application context (set by TenantFilter)
CREATE OR REPLACE FUNCTION app.current_tenant_id()
RETURNS UUID
LANGUAGE SQL
STABLE
AS $$
    SELECT NULLIF(current_setting('app.current_company_id', TRUE), '')::UUID;
$$;
