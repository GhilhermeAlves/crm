-- V071__agent_config_generation_fields.sql
-- Sprint 2 (IA autonomia robusta): parametros de geracao por empresa.
--
-- Cada novo campo possui uso real no fluxo (OpenAiChatProvider):
--   * model        -> body "model" (override do app.ai.model do deploy)
--   * temperature  -> body "temperature" (0.0-2.0)
--   * max_tokens   -> body "max_tokens" e orcamento do contexto (historia)
--
-- NULL = usa o default do provider (consistente com a config de infra app.ai.*).
-- Colunas em tabela EXISTENTE (V070): nao altera RLS/grants ja aplicados.

ALTER TABLE agent_config
    ADD COLUMN model        VARCHAR(120),
    ADD COLUMN temperature  NUMERIC(4,3),
    ADD COLUMN max_tokens   INTEGER;

ALTER TABLE agent_config
    ADD CONSTRAINT chk_agent_config_temperature
        CHECK (temperature IS NULL OR (temperature >= 0 AND temperature <= 2)),
    ADD CONSTRAINT chk_agent_config_max_tokens
        CHECK (max_tokens IS NULL OR max_tokens > 0);