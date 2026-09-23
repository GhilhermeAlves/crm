#!/usr/bin/env bash
# Backup diário dos bancos do CRM (crm + keycloak_db) — postgres:17-alpine.
# Uso: BACKUP_DIR=/opt/crm/backups RETENTION_DAYS=7 ./backup-db.sh
set -euo pipefail
POSTGRES_CONTAINER=${POSTGRES_CONTAINER:-crm-postgres}
POSTGRES_USER=${POSTGRES_USER:-crm_admin}
BACKUP_DIR=${BACKUP_DIR:-/opt/crm/backups}
RETENTION_DAYS=${RETENTION_DAYS:-7}
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p "$BACKUP_DIR"
for DB in crm keycloak_db; do
  docker exec "$POSTGRES_CONTAINER" pg_dump -U "$POSTGRES_USER" -d "$DB" --format=custom -f "/tmp/${DB}.dump"
  docker cp "$POSTGRES_CONTAINER:/tmp/${DB}.dump" "$BACKUP_DIR/${DB}-${STAMP}.dump"
  docker exec "$POSTGRES_CONTAINER" rm -f "/tmp/${DB}.dump"
done
# Rotação
find "$BACKUP_DIR" -name '*.dump' -mtime "+${RETENTION_DAYS}" -delete
echo "Backup concluído em $BACKUP_DIR ($(date))"
