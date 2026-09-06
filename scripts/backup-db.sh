#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# VETRA PostgreSQL + PostGIS Backup Script (scripts/backup-db.sh)
# Creates a compressed, timestamped custom-format PostgreSQL dump file.
# ─────────────────────────────────────────────────────────────────────────────

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Source environment variables if .env exists
if [ -f .env ]; then
  # Export non-comment lines
  export $(grep -v '^#' .env | xargs)
fi

DB_CONTAINER="${DB_CONTAINER_NAME:-vetra-postgres}"
DB_NAME="${DB_NAME:-vetra_db}"
DB_USER="${DB_USER:-vetra_user}"
BACKUP_DIR="backups"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/vetra_db_backup_${TIMESTAMP}.dump"

mkdir -p "$BACKUP_DIR"

echo -e "${CYAN}[*] Starting backup of database '${DB_NAME}' from container '${DB_CONTAINER}'...${NC}"

# Check if PostgreSQL container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo -e "${RED}[ERROR] Container '${DB_CONTAINER}' is not currently running!${NC}"
  exit 1
fi

# Execute pg_dump inside the container and stream output to compressed file
docker exec -t "$DB_CONTAINER" pg_dump -U "$DB_USER" -d "$DB_NAME" -Fc --clean --if-exists > "$BACKUP_FILE"

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)

echo -e "${GREEN}================================================================${NC}"
echo -e "${GREEN}  ✓ BACKUP CREATED SUCCESSFULLY!                                ${NC}"
echo -e "  • File: ${CYAN}${BACKUP_FILE}${NC}"
echo -e "  • Size: ${CYAN}${FILE_SIZE}${NC}"
echo -e "${GREEN}================================================================${NC}"
echo ""
echo -e "${YELLOW}To restore this backup in the future, run:${NC}"
echo -e "  ${CYAN}./scripts/restore-db.sh ${BACKUP_FILE}${NC}"
echo ""
