-- V075__agent_config_temperature_double_precision.sql
-- Alinha o tipo fisico de agent_config.temperature ao mapeamento JPA (Double),
-- corrigindo o schema validation do Spring Boot (ddl-auto=validate) em producao.
--
-- A V071 criou a coluna como NUMERIC(4,3); o dominio/entidade usa Double e o
-- cenário de testes (Hibernate create-drop) gera DOUBLE PRECISION. Essa
-- divergencia fazia o boot em producao falhar com
-- "wrong column type encountered in column [temperature] ... expecting float(53)".
--
-- Valores 0.0..2.0 armazenados em NUMERIC(4,3) sao representaveis sem perda em
-- DOUBLE PRECISION. A constraint chk_agent_config_temperature (NULL ou 0..2),
-- RLS e demais grants sao preservados (ALTER COLUMN TYPE nao os afeta).
ALTER TABLE agent_config
    ALTER COLUMN temperature TYPE DOUBLE PRECISION;