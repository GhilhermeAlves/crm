-- V125: público SELECTED — contatos escolhidos um a um na campanha.
-- audience_criteria guarda {"contactIds": [...]}; só o CHECK de audience_type muda.

DO $$
DECLARE
    c text;
BEGIN
    FOR c IN
        SELECT con.conname
          FROM pg_constraint con
          JOIN pg_class rel ON rel.oid = con.conrelid
         WHERE rel.relname = 'campaigns'
           AND con.contype = 'c'
           AND pg_get_constraintdef(con.oid) LIKE '%audience_type%'
    LOOP
        EXECUTE format('ALTER TABLE campaigns DROP CONSTRAINT %I', c);
    END LOOP;
END $$;

ALTER TABLE campaigns
    ADD CONSTRAINT campaigns_audience_type_check
    CHECK (audience_type IN ('CONTACTS', 'LEADS', 'SELECTED'));
