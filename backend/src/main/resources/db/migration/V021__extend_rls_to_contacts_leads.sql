-- V021__extend_rls_to_contacts_leads.sql
-- Estende RLS para as tabelas de CRM criadas em V015/V016
-- (contacts, contact_addresses, contact_custom_fields, tags, contact_tags, leads)
-- REQUER: V013 (app.current_tenant_id), V019/V020 (padrão de policies)
--
-- Tabelas com company_id direto:
--   contacts, tags, leads
-- Tabelas relacionadas (sem company_id), tenant resolvido via pai:
--   contact_addresses      -> contacts.company_id
--   contact_custom_fields  -> contacts.company_id
--   contact_tags           -> contacts.company_id E tags.company_id

-- ===========================================================================
-- 1. Habilitar e FORCAR RLS nas seis tabelas
-- ===========================================================================
ALTER TABLE contacts ENABLE ROW LEVEL SECURITY;
ALTER TABLE contacts FORCE ROW LEVEL SECURITY;

ALTER TABLE contact_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact_addresses FORCE ROW LEVEL SECURITY;

ALTER TABLE contact_custom_fields ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact_custom_fields FORCE ROW LEVEL SECURITY;

ALTER TABLE tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE tags FORCE ROW LEVEL SECURITY;

ALTER TABLE contact_tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact_tags FORCE ROW LEVEL SECURITY;

ALTER TABLE leads ENABLE ROW LEVEL SECURITY;
ALTER TABLE leads FORCE ROW LEVEL SECURITY;

-- ===========================================================================
-- 2. Policies de isolamento por tenant
-- ===========================================================================
-- contacts: company_id direto
CREATE POLICY tenant_isolation_policy ON contacts
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- contact_addresses: via contact.company_id
CREATE POLICY tenant_isolation_policy ON contact_addresses
    USING (
        EXISTS (
            SELECT 1 FROM contacts c
            WHERE c.id = contact_addresses.contact_id
            AND c.company_id = app.current_tenant_id()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM contacts c
            WHERE c.id = contact_addresses.contact_id
            AND c.company_id = app.current_tenant_id()
        )
    );

-- contact_custom_fields: via contact.company_id
CREATE POLICY tenant_isolation_policy ON contact_custom_fields
    USING (
        EXISTS (
            SELECT 1 FROM contacts c
            WHERE c.id = contact_custom_fields.contact_id
            AND c.company_id = app.current_tenant_id()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM contacts c
            WHERE c.id = contact_custom_fields.contact_id
            AND c.company_id = app.current_tenant_id()
        )
    );

-- tags: company_id direto
CREATE POLICY tenant_isolation_policy ON tags
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- contact_tags: via contact.company_id E tag.company_id (ambos do tenant)
CREATE POLICY tenant_isolation_policy ON contact_tags
    USING (
        EXISTS (
            SELECT 1 FROM contacts c
            WHERE c.id = contact_tags.contact_id
            AND c.company_id = app.current_tenant_id()
        )
        AND EXISTS (
            SELECT 1 FROM tags t
            WHERE t.id = contact_tags.tag_id
            AND t.company_id = app.current_tenant_id()
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM contacts c
            WHERE c.id = contact_tags.contact_id
            AND c.company_id = app.current_tenant_id()
        )
        AND EXISTS (
            SELECT 1 FROM tags t
            WHERE t.id = contact_tags.tag_id
            AND t.company_id = app.current_tenant_id()
        )
    );

-- leads: company_id direto
CREATE POLICY tenant_isolation_policy ON leads
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- 3. Verificação final (18 tabelas tenant-scoped com RLS)
-- ===========================================================================
DO $$
DECLARE
    rls_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO rls_count
    FROM pg_class c
    JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE c.relkind = 'r'
    AND c.relrowsecurity = true
    AND n.nspname = 'public'
    AND c.relname IN (
        'users', 'roles', 'user_roles', 'audit_logs',
        'company_settings', 'subscriptions', 'pipelines',
        'stages', 'opportunities', 'opportunity_history',
        'refresh_tokens', 'password_reset_tokens',
        'contacts', 'contact_addresses', 'contact_custom_fields',
        'tags', 'contact_tags', 'leads'
    );

    IF rls_count != 18 THEN
        RAISE EXCEPTION 'RLS habilitado em % tabelas (esperado 18)', rls_count;
    END IF;

    RAISE NOTICE 'V021: RLS estendido com sucesso para 18 tabelas tenant-scoped';
END $$;
