-- V126__company_invitation_prefill_optional_cnpj.sql
--
-- 1. Convite de criação de empresa com dados pré-preenchidos: o CRM_ADMIN pode
--    adiantar razão social, nome fantasia, documento, contato e endereço; o
--    convidado só confere/corrige ao aceitar. Opcional (NULL = nada adiantado).
--    A tabela já tem RLS (V123) e GRANT para crm_app; a coluna nova herda ambos.
--
-- 2. Documento da empresa opcional: empresas podem operar com CPF ou ainda não
--    ter CNPJ. O índice único idx_companies_cnpj (V005) continua valendo para
--    quem informa o documento — no Postgres, vários NULL não colidem.

ALTER TABLE company_invitations ADD COLUMN IF NOT EXISTS company_prefill JSONB;

ALTER TABLE companies ALTER COLUMN cnpj DROP NOT NULL;
