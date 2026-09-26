#!/bin/bash
# =============================================================================
# PostgreSQL Init — Criação dos bancos de dados do CRM
# =============================================================================
# Este script é executado automaticamente na primeira inicialização do
# container PostgreSQL. Cria os bancos separados por domínio para
# viabilizar a arquitetura de microsserviços com isolamento de dados.
#
# Referência: docs/03-database/
# =============================================================================

set -e

echo ">>> Iniciando criação dos bancos de dados do CRM..."

# Lista de bancos por domínio
databases=(
  "auth_db"
  "user_db"
  "employee_db"
  "student_db"
  "finance_db"
  "billing_db"
  "academic_db"
  "enrollment_db"
  "tenant_db"
  "keycloak_db"
)

for db in "${databases[@]}"; do
  echo "  -> Criando banco: $db"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE $db'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

echo ">>> Todos os bancos criados com sucesso."

# Configurações globais
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  -- Extensão UUID
  CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

  -- Extensão para auditoria
  CREATE EXTENSION IF NOT EXISTS "pgcrypto";

  -- Garantir privilégios básicos
  GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO $POSTGRES_USER;
EOSQL

echo ">>> Configurações globais aplicadas."
