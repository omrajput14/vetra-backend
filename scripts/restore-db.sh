#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# VETRA PostgreSQL + PostGIS Restore Script (scripts/restore-db.sh)
# Restores a compressed custom-format PostgreSQL dump file into the database.
# Usage: ./scripts/restore-db.sh [backup_file_path]
# ─────────────────────────────────────────────────────────────────────────────

set -e

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Source environment variables if .env exists
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

DB_CONTAINER="${DB_CONTAINER_NAME:-vetra-postgres}"
DB_NAME="${DB_NAME:-vetra_db}"
DB_USER="${DB_USER:-vetra_user}"

BACKUP_FILE="$1"

if [ -z "$BACKUP_FILE" ]; then
  echo -e "${YELLOW}[!] No backup file specified. Listing available backups in backups/:${NC}"
  ls -lh backups/*.dump 2>/dev/null || true
  echo ""
  echo -e "${CYAN}Usage: ./scripts/restore-db.sh backups/<backup_file_name>.dump${NC}"
  exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo -e "${RED}[ERROR] Backup file '${BACKUP_FILE}' does not exist!${NC}"
  exit 1
fi

echo -e "${RED}================================================================${NC}"
echo -e "${YELLOW}  [WARNING] You are about to RESTORE database '${DB_NAME}'.      ${NC}"
echo -e "${YELLOW}  This will overwrite existing data with: ${BACKUP_FILE}       ${NC}"
echo -e "${RED}================================================================${NC}"
read -p "Are you sure you want to proceed? (y/N): " -r CONFIRM
if [[ ! "$CONFIRM" =~ ^[Yy]$ ]]; then
  echo -e "${CYAN}[*] Restore cancelled by user.${NC}"
  exit 0
fi

echo -e "${CYAN}[*] Restoring database '${DB_NAME}' from '${BACKUP_FILE}'...${NC}"

# Check if PostgreSQL container is running
if ! docker ps --format '{{.Names}}' | grep -q "^${DB_CONTAINER}$"; then
  echo -e "${RED}[ERROR] Container '${DB_CONTAINER}' is not currently running!${NC}"
  exit 1
fi

# Execute pg_restore inside container
docker exec -i "$DB_CONTAINER" pg_restore -U "$DB_USER" -d "$DB_NAME" --clean --if-exists < "$BACKUP_FILE" || true

echo -e "${GREEN}================================================================${NC}"
echo -e "${GREEN}  ✓ DATABASE RESTORE COMPLETED!                                 ${NC}"
echo -e "${GREEN}================================================================${NC}"
echo ""
