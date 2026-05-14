#!/bin/bash
# PostgreSQL daily backup script
# Deploy to: /home/deploy/backup-db.sh
# Cron: 0 3 * * * /home/deploy/backup-db.sh >> /home/deploy/backups/backup.log 2>&1
set -euo pipefail

BACKUP_DIR="/home/deploy/backups"
DATE=$(date +%Y%m%d_%H%M%S)
FILE="${BACKUP_DIR}/idlegame_${DATE}.sql.gz"
RETAIN_DAYS=7

# Get credentials from running container
POSTGRES_USER=$(docker exec shared-postgres env | grep POSTGRES_USER | cut -d= -f2)
POSTGRES_DB=$(docker exec shared-postgres env | grep POSTGRES_DB | cut -d= -f2)

docker exec shared-postgres pg_dump \
  -U "${POSTGRES_USER}" "${POSTGRES_DB}" \
  | gzip > "${FILE}"

find "${BACKUP_DIR}" -name "idlegame_*.sql.gz" \
  -mtime +${RETAIN_DAYS} -delete

echo "[$(date)] Backup OK: ${FILE} ($(du -h ${FILE} | cut -f1))"
