#!/usr/bin/env bash
# Restore pontual: ./restore-db.sh <banco> <arquivo.dump>
set -euo pipefail
DB=${1:?uso: restore-db.sh <banco> <arquivo.dump>}
DUMP=${2:?uso: restore-db.sh <banco> <arquivo.dump>}
docker cp "$DUMP" crm-postgres:/tmp/restore.dump
docker exec crm-postgres pg_restore -U crm_admin -d "$DB" --clean --if-exists /tmp/restore.dump
docker exec crm-postgres rm -f /tmp/restore.dump
echo "Restore de $DB concluído."
