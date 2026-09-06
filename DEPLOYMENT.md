# VETRA Backend — Azure Ubuntu VM Deployment Guide

This guide provides step-by-step instructions for deploying the **VETRA Backend** (Spring Boot 3.5 + PostgreSQL 17 with PostGIS + Redis) on a single **Ubuntu Virtual Machine on Microsoft Azure** using Docker and Docker Compose.

---

## 1. Architecture Overview

```text
                                  Internet
                                     │
                                     ▼
                      Azure Network Security Group (NSG)
                         ├── Port 22   (SSH Admin)
                         └── Port 8080 (Spring Boot REST API)
                                     │
                                     ▼
                        Azure Ubuntu VM (Public IP)
                                     │
    ┌────────────────────────────────┼────────────────────────────────┐
    │ Docker Host                    │                                │
    │                                ▼                                │
    │                  ┌───────────────────────────┐                  │
    │                  │  VETRA Spring Boot API    │                  │
    │                  │  (vetra-backend: Port 8080)│                 │
    │                  └─────────────┬─────────────┘                  │
    │                                │                                │
    │                      Docker Internal Network                    │
    │                         (vetra-network)                         │
    │                                │                                │
    │                ┌───────────────┴───────────────┐                │
    │                ▼                               ▼                │
    │   ┌──────────────────────────┐   ┌──────────────────────────┐   │
    │   │ PostgreSQL 17 + PostGIS  │   │ Redis 7 In-Memory Cache  │   │
    │   │ (postgres: Port 5432)    │   │ (redis: Port 6379)       │   │
    │   │ [PRIVATE — NOT EXPOSED]  │   │ [PRIVATE — NOT EXPOSED]  │   │
    │   └────────────┬─────────────┘   └─────────────┬────────────┘   │
    │                │                               │                │
    │                ▼                               ▼                │
    │   ┌──────────────────────────┐   ┌──────────────────────────┐   │
    │   │ Volume: postgres_data    │   │ Volume: redis_data       │   │
    │   └──────────────────────────┘   └──────────────────────────┘   │
    └─────────────────────────────────────────────────────────────────┘
```

### Security Policy:
* **Publicly Exposed:**
  * `Port 22` (SSH) — Remote administration (restricted to your IP recommended).
  * `Port 8080` (HTTP) — Spring Boot REST API & Swagger UI.
* **Internal / Strictly Private (Never exposed to host or internet):**
  * `Port 5432` — PostgreSQL database with PostGIS.
  * `Port 6379` — Redis caching engine.

---

## 2. Azure Virtual Machine Specifications & Setup

### A. Recommended VM Sizing (Azure for Students Compatible):
* **OS:** Ubuntu Server 22.04 LTS or 24.04 LTS (x64 Architecture).
* **Size:** `Standard_B2s` (2 vCPUs, 4 GB RAM) or `Standard_B2ms` (2 vCPUs, 8 GB RAM).
* **Storage:** 30 GB - 50 GB Premium SSD.
* **Authentication Type:** SSH Public Key.

### B. Azure Network Security Group (NSG) Rules:
In the Azure Portal, navigate to your VM's **Network settings** and ensure the following **Inbound port rules** are created:

| Priority | Name | Port | Protocol | Source | Action | Description |
| :---: | :--- | :---: | :---: | :---: | :---: | :--- |
| **1000** | `Allow-SSH` | `22` | TCP | `Any` (or `My IP`) | **Allow** | SSH Terminal Access |
| **1010** | `Allow-Vetra-API` | `8080` | TCP | `Any` | **Allow** | Spring Boot REST Endpoints |
| **65000**| `DenyAllInbound` | `*` | Any | `Any` | **Deny** | Blocks 5432, 6379, etc. |

---

## 3. Connecting to Your Azure VM

From your local machine, open your terminal and connect via SSH:

```bash
# Connect using your private key and the VM's Public IP
ssh -i ~/.ssh/id_rsa azureuser@<YOUR_VM_PUBLIC_IP>
```

---

## 4. Application Deployment Steps

### Step 1: Clone the Repository
```bash
git clone https://github.com/omrajput14/vetra-backend.git
cd vetra-backend
```

### Step 2: Run the Automated VM Setup Script
The `setup-vm.sh` script automatically installs Docker, Docker Compose, system dependencies, and configures the host firewall (UFW):

```bash
chmod +x setup-vm.sh
./setup-vm.sh
```

### Step 3: Refresh Docker Permissions
Apply the new Docker group membership to your session:
```bash
newgrp docker
```

### Step 4: Configure Environment Variables
Create your production `.env` file from the provided template:

```bash
cp .env.example .env
nano .env
```

*Update the following key secrets in `.env`:*
* `DB_PASSWORD`: Set a strong PostgreSQL database password.
* `REDIS_PASSWORD`: Set a strong Redis cache password.
* `JWT_SECRET`: Set a random 256-bit secret string (minimum 32 characters).
* `GEMINI_API_KEY`: Set your Google Gemini API key for AI livestock diagnostics.

Save and exit (`Ctrl+O`, `Enter`, `Ctrl+X`).

### Step 5: Launch the Platform with the Deployment Script
Run the automated deployment script:

```bash
chmod +x deploy.sh
./deploy.sh
```

The script will:
1. Build the production multi-stage Docker container.
2. Launch PostgreSQL with PostGIS, Redis, and Spring Boot.
3. Automatically apply all 29 Flyway database migrations (`V1` to `V29`).
4. Wait for the Spring Boot Actuator healthcheck (`/actuator/health`) to pass.
5. Print your live API URLs and status table.

---

## 5. Verification & Health Checks

### A. Test Health Endpoint from the Terminal:
```bash
curl -s http://localhost:8080/actuator/health | jq .
```
*Expected Output:*
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

### B. Verify PostGIS Spatial Extensions:
Verify that PostGIS is active inside the PostgreSQL container:
```bash
docker compose exec postgres psql -U vetra_user -d vetra_db -c "SELECT extname, extversion FROM pg_extension;"
```
*Expected Output:*
```text
  extname  | extversion 
-----------+------------
 plpgsql   | 1.0
 uuid-ossp | 1.4
 postgis   | 3.5.0
 pg_trgm   | 1.6
(4 rows)
```

### C. Test Public Access from your Browser / Mobile:
* **API Health:** `http://<YOUR_VM_PUBLIC_IP>:8080/actuator/health`
* **Swagger UI:** `http://<YOUR_VM_PUBLIC_IP>:8080/swagger-ui.html`
* **API Documentation:** `http://<YOUR_VM_PUBLIC_IP>:8080/v3/api-docs`

---

## 6. Daily Operations & Docker Management

### View Real-Time Application Logs:
```bash
# Follow backend Spring Boot logs
docker compose logs -f vetra-backend

# Follow all container logs
docker compose logs -f
```

### Check Container Health & Uptime:
```bash
docker compose ps
```

### Restart Backend or Entire Stack:
```bash
# Restart only Spring Boot
docker compose restart vetra-backend

# Restart entire platform
docker compose restart
```

### Rebuild and Deploy After Code Updates:
```bash
# Pull latest code from git and redeploy
./deploy.sh --pull
```

### Stop the Platform:
```bash
# Stop containers (preserves database data volume)
docker compose down
```

---

## 7. Database Backup & Restore

### A. Create an On-Demand Backup:
Run the automated backup script to create a compressed, timestamped dump in `backups/`:
```bash
./scripts/backup-db.sh
```
*Output:* `backups/vetra_db_backup_YYYYMMDD_HHMMSS.dump`

### B. Restore a Database Backup:
```bash
./scripts/restore-db.sh backups/vetra_db_backup_20260901_120000.dump
```

### C. Automated Daily Backups via Cron:
To schedule automated backups every night at 2:00 AM:
```bash
crontab -e
```
Add the following line:
```bash
0 2 * * * cd /home/azureuser/vetra-backend && ./scripts/backup-db.sh >> /var/log/vetra-backup.log 2>&1
```

---

## 8. Troubleshooting Guide

| Problem | Cause | Solution |
| :--- | :--- | :--- |
| `Healthcheck timed out` | Spring Boot is taking longer than 90s to initialize or DB connection failed | Run `docker compose logs -f vetra-backend` to inspect the startup stack trace. |
| `Cannot connect to PostgreSQL` | Database container not ready or bad password in `.env` | Verify `docker compose ps` shows `vetra-postgres` is `healthy`. Check `DB_PASSWORD` in `.env`. |
| `Permission denied while trying to connect to Docker daemon` | User is not in the `docker` group | Run `sudo usermod -aG docker $USER && newgrp docker`. |
| `Port 8080 unreachable externally` | Azure NSG firewall blocking port 8080 | Check your Azure NSG inbound security rules and ensure port 8080 is allowed from `Any`. |
