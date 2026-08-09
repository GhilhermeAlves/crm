-- V031__memberships_app_grants.sql
-- Sprint 8.2 - Membership
-- Concede DML no memberships ao usuario de aplicacao (crm_app), NOBYPASSRLS.
-- Necessario para o Hibernate/app consultar a tabela (paridade com as demais
-- tabelas RLS, ex.: users). Idempotente; seguro reaplicar.
GRANT SELECT, INSERT, UPDATE, DELETE ON memberships TO crm_app;
