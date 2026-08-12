-- V037__company_quota_storage.sql
-- Sprint 8.6 - SaaS Hardening / Enforcement de quotas por plano
--
-- Rastreia o armazenamento usado por empresa (max_storage_mb) de forma
-- tenanted e isolada por RLS. Cada upload registra um objeto com o tamanho em
-- bytes; o uso corrente é a soma de size_bytes por company_id.
--
-- Nota de design: não existia infraestrutura de upload/MinIO no projeto. Esta
-- tabela é a infraestrutura mínima reutilizável (blob em banco) para aplicar e
-- testar a quota; o port de storage permite trocar por object-store externo
-- (ex.: MinIO) no futuro sem mexer nos casos de uso.

-- ===========================================================================
-- 1. Tabela storage_objects
-- ===========================================================================
CREATE TABLE storage_objects (
    id           uuid         NOT NULL DEFAULT gen_random_uuid(),
    company_id   uuid         NOT NULL,
    object_key   varchar(255) NOT NULL,
    file_name    varchar(255) NOT NULL,
    content_type varchar(120),
    size_bytes   bigint       NOT NULL CHECK (size_bytes >= 0),
    data         bytea,
    created_by   uuid,
    created_at   timestamp    NOT NULL DEFAULT now(),
    CONSTRAINT storage_objects_pkey PRIMARY KEY (id),
    CONSTRAINT fk_storage_objects_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_storage_objects_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_storage_objects_company_key UNIQUE (company_id, object_key)
);

CREATE INDEX idx_storage_objects_company_id ON storage_objects(company_id);

-- ===========================================================================
-- 2. RLS FORCE + policy de isolamento por tenant (padrão V019/V021)
-- ===========================================================================
ALTER TABLE storage_objects ENABLE ROW LEVEL SECURITY;
ALTER TABLE storage_objects FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON storage_objects
    USING (company_id = app.current_tenant_id())
    WITH CHECK (company_id = app.current_tenant_id());

-- ===========================================================================
-- 3. Grants (padrão V031/V034)
-- ===========================================================================
GRANT SELECT, INSERT, UPDATE, DELETE ON storage_objects TO crm_app;

-- ===========================================================================
-- 4. Verificação estrutural (pg_catalog, imune a RLS)
-- ===========================================================================
DO $$
DECLARE
    has_rls BOOLEAN;
    is_forced BOOLEAN;
    has_policy BOOLEAN;
BEGIN
    SELECT c.relrowsecurity INTO has_rls
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'storage_objects';

    SELECT c.relforcerowsecurity INTO is_forced
    FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid
    WHERE n.nspname = 'public' AND c.relname = 'storage_objects';

    SELECT EXISTS (SELECT 1 FROM pg_policies
                   WHERE schemaname = 'public' AND tablename = 'storage_objects'
                     AND policyname = 'tenant_isolation_policy') INTO has_policy;

    IF NOT has_rls THEN RAISE EXCEPTION 'V037: RLS ausente em storage_objects'; END IF;
    IF NOT is_forced THEN RAISE EXCEPTION 'V037: storage_objects sem FORCE ROW LEVEL SECURITY'; END IF;
    IF NOT has_policy THEN RAISE EXCEPTION 'V037: policy tenant_isolation_policy ausente em storage_objects'; END IF;

    RAISE NOTICE 'V037: storage_objects criado (RLS FORCE + policy isolamento ok)';
END $$;