-- ===========================================================================
-- V109 — Procedimento "A realizar" guarda se deve gerar o lançamento financeiro
-- quando for marcado como realizado (ficha clínica). Coluna nova em tabela que
-- já tem RLS FORCE e GRANT (V106); nada a acrescentar de política.
-- ===========================================================================
ALTER TABLE patient_procedures
    ADD COLUMN generate_transaction_on_done BOOLEAN NOT NULL DEFAULT FALSE;
