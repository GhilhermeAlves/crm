-- Pós Sprint 8.4: corrige o trigger que não sincronizava users.company_id
-- quando a PRIMEIRA membership ACTIVE era criada.
--
-- Causa: o subquery (NOT EXISTS ... status='ACTIVE') enxergava a própria linha
-- recém-inserida no AFTER ROW trigger, então a condição nunca era satisfeita
-- para a primeira membership e users.company_id jamais era atualizado.
--
-- Correção: excluir o registro corrente (m.id <> NEW.id) da checagem.
CREATE OR REPLACE FUNCTION app.membership_sync_active_company() RETURNS trigger
LANGUAGE plpgsql
AS $function$
BEGIN
    IF NEW.status = 'ACTIVE' THEN
        UPDATE users
        SET company_id = NEW.company_id, updated_at = NOW()
        WHERE id = NEW.user_id
          AND NOT EXISTS (
              SELECT 1 FROM memberships m
              WHERE m.user_id = NEW.user_id
                AND m.status = 'ACTIVE'
                AND m.id <> NEW.id
          );
    END IF;
    RETURN COALESCE(NEW, OLD);
END $function$;