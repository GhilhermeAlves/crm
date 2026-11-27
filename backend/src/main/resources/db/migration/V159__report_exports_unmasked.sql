-- V159__report_exports_unmasked.sql
-- Mascaramento LGPD nas exportações: grava, no pedido, se o solicitante podia
-- ver dados pessoais sem máscara (permissão person:sensitive ou CRM_ADMIN).
-- O gerador assíncrono não tem o usuário logado e decide pela coluna. Pedidos
-- antigos ficam mascarados (FALSE). RLS/GRANT de report_exports não mudam (V096).

ALTER TABLE report_exports ADD COLUMN IF NOT EXISTS unmasked BOOLEAN NOT NULL DEFAULT FALSE;
