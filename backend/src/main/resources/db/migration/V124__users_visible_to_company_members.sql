-- V124: membros de outra empresa ativa aparecem na lista de usuários.
--
-- tenant_isolation_policy (V019) só mostra usuários cuja empresa ATIVA
-- (users.company_id) é o tenant atual. Quem é membro de várias empresas sumia
-- da lista de membros (JOIN com users) mas seguia contando no max_users
-- (count só em memberships) — limite atingido com a lista "vazia".
--
-- Esta policy (permissiva, soma às demais) libera SELECT dos usuários que têm
-- membership no tenant atual. A checagem roda numa função SECURITY DEFINER
-- (criada pelo superuser das migrations, ignora RLS) para não recursar com
-- membership_own_policy, que consulta users.

CREATE OR REPLACE FUNCTION app.is_member_of_current_tenant(p_user_id uuid)
RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM memberships m
        WHERE m.user_id = p_user_id
          AND m.company_id = app.current_tenant_id()
    );
$$;

REVOKE ALL ON FUNCTION app.is_member_of_current_tenant(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app.is_member_of_current_tenant(uuid) TO crm_app;

DROP POLICY IF EXISTS users_company_member_select_policy ON users;
CREATE POLICY users_company_member_select_policy ON users
    FOR SELECT
    USING (app.is_member_of_current_tenant(id));
