#!/bin/bash
# =============================================================================
# PostgreSQL Init — role de aplicação crm_app
# =============================================================================
# O backend conecta como crm_app (SPRING_DATASOURCE_USERNAME) e as migrations
# (V031+) fazem GRANT ... TO crm_app. Sem a role, o Flyway falha em banco novo.
# Atributos iguais aos de produção: LOGIN, NOSUPERUSER, NOBYPASSRLS (o RLS das
# tabelas de tenant vale para ela; só crm_admin faz bypass).
#
# Roda só na PRIMEIRA inicialização do volume. A senha vem de CRM_APP_PASSWORD
# (se passada ao container); sem ela a role é criada sem senha e o log avisa
# como defini-la — deve ser a mesma de SPRING_DATASOURCE_PASSWORD no .env.
# =============================================================================
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  DO \$\$
  BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'crm_app') THEN
      CREATE ROLE crm_app LOGIN NOSUPERUSER NOBYPASSRLS;
    END IF;
  END \$\$;
EOSQL

if [ -n "${CRM_APP_PASSWORD:-}" ]; then
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    -v pw="$CRM_APP_PASSWORD" -c "ALTER ROLE crm_app PASSWORD :'pw'"
  echo ">>> Role crm_app criada com senha."
else
  echo ">>> ATENÇÃO: role crm_app criada SEM senha. Defina com:"
  echo ">>>   docker exec -it crm-postgres psql -U $POSTGRES_USER -d $POSTGRES_DB \\"
  echo ">>>     -c \"ALTER ROLE crm_app PASSWORD '<SPRING_DATASOURCE_PASSWORD do .env>'\""
fi
