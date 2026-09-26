#!/usr/bin/env bash
# Backup dos bancos do CRM (crm_main + keycloak_db) — postgres:17-alpine.
# Uso: BACKUP_DIR=/opt/crm/backups RETENTION_DAYS=7 ./backup-db.sh
# Cron (VPS): 0 3 * * * /opt/crm/scripts/backup-db.sh >> /var/log/crm-db-backup.log 2>&1
set -euo pipefail
POSTGRES_CONTAINER=${POSTGRES_CONTAINER:-crm-postgres}
POSTGRES_USER=${POSTGRES_USER:-crm_admin}
BACKUP_DIR=${BACKUP_DIR:-/opt/crm/backups}
RETENTION_DAYS=${RETENTION_DAYS:-7}
DATABASES=${DATABASES:-"crm_main keycloak_db"}
STAMP=$(date +%Y%m%d-%H%M%S)
mkdir -p "$BACKUP_DIR"
for DB in $DATABASES; do
  docker exec "$POSTGRES_CONTAINER" pg_dump -U "$POSTGRES_USER" -d "$DB" --format=custom -f "/tmp/${DB}.dump"
  docker cp "$POSTGRES_CONTAINER:/tmp/${DB}.dump" "$BACKUP_DIR/${DB}-${STAMP}.dump"
  docker exec "$POSTGRES_CONTAINER" rm -f "/tmp/${DB}.dump"
  # Dump vazio/truncado = backup inválido: falha alto em vez de rotacionar os bons.
  [ -s "$BACKUP_DIR/${DB}-${STAMP}.dump" ] || { echo "ERRO: dump vazio de $DB" >&2; exit 1; }
done
# Rotação
find "$BACKUP_DIR" -name '*.dump' -mtime "+${RETENTION_DAYS}" -delete
echo "Backup concluído em $BACKUP_DIR ($(date))"
