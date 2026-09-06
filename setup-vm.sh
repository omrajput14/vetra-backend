#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# VETRA Azure VM Provisioning Script (setup-vm.sh)
# Target OS: Ubuntu 22.04 / 24.04 LTS on Microsoft Azure
# Purpose: Installs Docker, Docker Compose, system tools, and configures firewall.
# ─────────────────────────────────────────────────────────────────────────────

set -e

# Color definitions for console output
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}================================================================${NC}"
echo -e "${GREEN}      VETRA AZURE UBUNTU VM AUTOMATED SETUP SCRIPT             ${NC}"
echo -e "${CYAN}================================================================${NC}"

# Check for root / sudo privileges
if [ "$EUID" -ne 0 ]; then
  echo -e "${YELLOW}[!] This script requires administrative privileges.${NC}"
  echo -e "${CYAN}[*] Re-running with sudo...${NC}"
  exec sudo bash "$0" "$@"
fi

# Determine actual non-root user
TARGET_USER="${SUDO_USER:-$USER}"

echo -e "${CYAN}[1/5] Updating Ubuntu package repositories...${NC}"
apt-get update -y
apt-get install -y --no-install-recommends \
    apt-transport-https \
    ca-certificates \
    curl \
    gnupg \
    lsb-release \
    git \
    jq \
    ufw \
    wget \
    tar \
    gzip

echo -e "${CYAN}[2/5] Installing Official Docker Engine & Docker Compose Plugin...${NC}"
# Remove any conflicting legacy packages
apt-get remove -y docker docker-engine docker.io containerd runc 2>/dev/null || true

# Add Docker's official GPG key
install -m 0755 -d /etc/apt/keyrings
if [ ! -f /etc/apt/keyrings/docker.gpg ]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
fi

# Set up the stable repository
UBUNTU_CODENAME="$(lsb_release -cs)"
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${UBUNTU_CODENAME} stable" > /etc/apt/sources.list.d/docker.list

# Install Docker packages
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable and start Docker service
systemctl enable docker
systemctl start docker

echo -e "${CYAN}[3/5] Configuring user permissions for Docker...${NC}"
if [ -n "$TARGET_USER" ] && [ "$TARGET_USER" != "root" ]; then
  usermod -aG docker "$TARGET_USER"
  echo -e "${GREEN}[✓] User '${TARGET_USER}' added to 'docker' group.${NC}"
fi

echo -e "${CYAN}[4/5] Configuring UFW Host Firewall (Security Hardening)...${NC}"
# Configure UFW default policies
ufw default deny incoming
ufw default allow outgoing

# Allow OpenSSH (Port 22) - Explicitly allowed BEFORE enabling UFW to prevent SSH lockout
ufw allow 22/tcp comment 'SSH Remote Access'
ufw allow OpenSSH comment 'SSH Remote Access Profile' 2>/dev/null || true

# Allow VETRA Spring Boot Application (Port 8080)
ufw allow 8080/tcp comment 'VETRA Backend API'

# Deny external access to PostgreSQL (5432) and Redis (6379)
ufw deny 5432/tcp comment 'PostgreSQL (Internal Docker Only)' 2>/dev/null || true
ufw deny 6379/tcp comment 'Redis (Internal Docker Only)' 2>/dev/null || true

# Enable UFW non-interactively
echo "y" | ufw enable || true
ufw status verbose

echo -e "${CYAN}[5/5] Verifying Installations...${NC}"
DOCKER_VER=$(docker --version)
COMPOSE_VER=$(docker compose version)

echo -e "${GREEN}================================================================${NC}"
echo -e "${GREEN}  ✓ DOCKER INSTALLED:  ${DOCKER_VER}${NC}"
echo -e "${GREEN}  ✓ COMPOSE INSTALLED: ${COMPOSE_VER}${NC}"
echo -e "${GREEN}  ✓ FIREWALL:          Ports 22 (SSH) and 8080 (API) OPEN${NC}"
echo -e "${GREEN}                       Port 5432 (PostgreSQL) BLOCKED to public${NC}"
echo -e "${GREEN}================================================================${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo -e "1. If you ran this via SSH, apply the docker group by running: ${CYAN}newgrp docker${NC} or logging out and back in."
echo -e "2. Configure your environment: ${CYAN}cp .env.example .env && nano .env${NC}"
echo -e "3. Launch the platform: ${CYAN}./deploy.sh${NC}"
echo ""
