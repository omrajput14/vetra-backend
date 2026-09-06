#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# VETRA Automated Deployment Script (deploy.sh)
# Usage: ./deploy.sh [--pull] [--no-cache]
# ─────────────────────────────────────────────────────────────────────────────

set -e

# Color definitions
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${CYAN}================================================================${NC}"
echo -e "${GREEN}      VETRA BACKEND PRODUCTION DEPLOYMENT SCRIPT               ${NC}"
echo -e "${CYAN}================================================================${NC}"

# 1. Check for .env configuration file
if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    echo -e "${YELLOW}[!] No .env file found. Creating from .env.example...${NC}"
    cp .env.example .env
    echo -e "${YELLOW}[!] Created .env with default values. Please customize secrets in .env!${NC}"
  else
    echo -e "${RED}[ERROR] Neither .env nor .env.example was found! Aborting.${NC}"
    exit 1
  fi
fi

# 2. Check for optional git pull flag
if [[ "$*" == *"--pull"* ]]; then
  echo -e "${CYAN}[*] Pulling latest changes from git repository...${NC}"
  git pull --rebase || true
fi

# 3. Build Docker container images
echo -e "${CYAN}[1/4] Building Docker container images...${NC}"
BUILD_FLAGS=""
if [[ "$*" == *"--no-cache"* ]]; then
  BUILD_FLAGS="--no-cache"
fi

docker compose build $BUILD_FLAGS

# 4. Start Docker Compose stack in detached mode
echo -e "${CYAN}[2/4] Starting PostgreSQL, Redis, and Spring Boot Backend...${NC}"
docker compose up -d

# 5. Wait for Spring Boot Actuator Healthcheck
echo -e "${CYAN}[3/4] Waiting for backend services to become healthy...${NC}"
MAX_RETRIES=30
RETRY_COUNT=0
HEALTH_URL="http://localhost:8080/actuator/health"
HEALTH_PASSED=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
  RETRY_COUNT=$((RETRY_COUNT + 1))
  
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
  
  if [ "$HTTP_STATUS" == "200" ]; then
    HEALTH_PASSED=true
    break
  fi
  
  echo -e "    Attempt $RETRY_COUNT/$MAX_RETRIES: Backend starting up (HTTP $HTTP_STATUS)... waiting 3s"
  sleep 3
done

echo ""
# 6. Display container status and verification
if [ "$HEALTH_PASSED" = true ]; then
  # Fetch detailed health components
  HEALTH_JSON=$(curl -s "$HEALTH_URL" 2>/dev/null || echo "{}")
  DB_STATUS=$(echo "$HEALTH_JSON" | jq -r '.components.db.status // .status' 2>/dev/null || echo "UNKNOWN")
  REDIS_STATUS=$(echo "$HEALTH_JSON" | jq -r '.components.redis.status // .status' 2>/dev/null || echo "UNKNOWN")

  echo -e "${GREEN}================================================================${NC}"
  echo -e "${GREEN}  ✓ DEPLOYMENT SUCCESSFUL! VETRA BACKEND IS HEALTHY & ONLINE   ${NC}"
  echo -e "${GREEN}================================================================${NC}"
  echo -e "  • Core Backend:     ${GREEN}UP (HTTP 200)${NC}"
  echo -e "  • PostgreSQL DB:    ${GREEN}${DB_STATUS}${NC}"
  echo -e "  • Redis Cache:      ${GREEN}${REDIS_STATUS}${NC}"
  echo ""
  
  # Verify PostGIS extensions in PostgreSQL
  echo -e "${CYAN}[*] Verifying PostgreSQL + PostGIS extensions...${NC}"
  POSTGIS_CHECK=$(docker compose exec -T postgres psql -U "${DB_USER:-vetra_user}" -d "${DB_NAME:-vetra_db}" -t -c "SELECT string_agg(extname, ', ') FROM pg_extension;" 2>/dev/null || echo "")
  if [ -n "$POSTGIS_CHECK" ]; then
    echo -e "  • Extensions Active: ${GREEN}${POSTGIS_CHECK}${NC}"
  fi
  echo ""

  docker compose ps
  echo ""
  
  # Fetch public IP if available
  PUBLIC_IP=$(curl -s --connect-timeout 2 https://ifconfig.me 2>/dev/null || echo "VM_PUBLIC_IP")
  
  echo -e "${CYAN}Public Endpoints:${NC}"
  echo -e "  • Base API:        ${GREEN}http://${PUBLIC_IP}:8080/api/v1${NC}"
  echo -e "  • Health Check:    ${GREEN}http://${PUBLIC_IP}:8080/actuator/health${NC}"
  echo -e "  • Swagger UI:      ${GREEN}http://${PUBLIC_IP}:8080/swagger-ui.html${NC}"
  echo -e "  • OpenAPI JSON:    ${GREEN}http://${PUBLIC_IP}:8080/v3/api-docs${NC}"
  echo ""
  echo -e "${YELLOW}Useful Operations Commands:${NC}"
  echo -e "  • View Live Logs:  ${CYAN}docker compose logs -f vetra-backend${NC}"
  echo -e "  • Restart Stack:   ${CYAN}docker compose restart${NC}"
  echo -e "  • Stop Platform:   ${CYAN}docker compose down${NC}"
  echo -e "  • Backup Database: ${CYAN}./scripts/backup-db.sh${NC}"
  echo ""
else
  echo -e "${RED}================================================================${NC}"
  echo -e "${RED}  [!] WARNING: Healthcheck timed out after 90 seconds.           ${NC}"
  echo -e "${RED}  Displaying recent container logs for diagnosis:               ${NC}"
  echo -e "${RED}================================================================${NC}"
  echo ""
  docker compose ps
  echo ""
  echo -e "${YELLOW}--- Spring Boot Backend Logs (Last 40 lines) ---${NC}"
  docker compose logs --tail 40 vetra-backend
  echo ""
  echo -e "${YELLOW}--- PostgreSQL Container Logs (Last 20 lines) ---${NC}"
  docker compose logs --tail 20 postgres
  echo ""
  exit 1
fi
