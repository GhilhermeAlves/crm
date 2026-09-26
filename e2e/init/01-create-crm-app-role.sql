-- e2e/init/01-create-crm-app-role.sql
-- Role de aplicação usada em produção (SPRING_DATASOURCE_USERNAME=crm_app).
--
-- As migrations (V031+) fazem GRANT ... TO crm_app; sem a role, o Flyway falha
-- com `role "crm_app" does not exist`. Em produção ela foi criada fora das
-- migrations, com os mesmos atributos: LOGIN, sem SUPERUSER, NOBYPASSRLS (o RLS
-- das tabelas de tenant vale para ela).
--
-- O backend do E2E conecta como crm_admin; aqui a role só precisa existir.
CREATE ROLE crm_app LOGIN PASSWORD 'crm_app' NOSUPERUSER NOBYPASSRLS;
