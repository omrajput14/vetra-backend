# 🐄 Vetra — Backend API Service

> **Spring Boot REST API** powering the Vetra livestock management and veterinary healthcare platform.

[![Java](https://img.shields.io/badge/Java-17-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9-red)](https://maven.apache.org/)

---

## 📋 Overview

Vetra Backend is a **production-quality RESTful API** built with Spring Boot 3 and PostgreSQL. It powers:

- **JWT-based dual-role authentication** (Farmer & Veterinarian)
- **Animal management** — register, update, track livestock
- **Appointment management** — booking, state machine workflow, optimistic locking
- **Electronic Veterinary Medical Records (EVMR)** — immutable clinical history
- **Disease reporting and outbreak tracking**
- **Dashboard metrics** for both Farmer and Veterinarian roles

---

## 📦 Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 17 |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL 15 |
| Migrations | Flyway |
| Security | Spring Security + JWT |
| Build | Maven 3.9 |
| Containers | Docker + Docker Compose |
| Code Style | Checkstyle |

---

## 🚀 Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- Docker & Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/omrajput14/vetra-backend.git
cd vetra-backend
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env with your database credentials
```

### 3. Start PostgreSQL via Docker

```bash
docker compose -f docker-compose.dev.yml up -d
```

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 5. Run tests

```bash
./mvnw test
```

---

## 🗄️ Database Migrations

Flyway manages all schema changes:

| Version | Description |
|---|---|
| V1 | Initial schema |
| V2 | Schema entities (Animal, User, VetProfile, FarmerProfile) |
| V3 | Refresh token table |
| V4 | Add animal name column |
| V5 | Appointment management tables |
| V6 | Medical records table |

---

## 🔑 Authentication

JWT-based authentication with refresh token support.

| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/auth/register/farmer` | POST | Public | Register as farmer |
| `/api/v1/auth/register/vet` | POST | Public | Register as veterinarian |
| `/api/v1/auth/login` | POST | Public | Login (farmer or vet) |
| `/api/v1/auth/refresh` | POST | Public | Refresh access token |
| `/api/v1/auth/profile` | GET/PUT | Authenticated | Get/update profile |
| `/api/v1/auth/vets` | GET | Authenticated | List veterinarians |

---

## 📋 API Endpoints

### Animals
| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/animals` | GET, POST | Farmer | List / create animals |
| `/api/v1/animals/{id}` | GET, PUT, DELETE | Farmer | Manage animal |

### Appointments
| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/appointments` | GET, POST | Farmer/Vet | List / create appointments |
| `/api/v1/appointments/{id}` | GET | Farmer/Vet | Get appointment |
| `/api/v1/appointments/{id}/status` | PUT | Vet | Update appointment status |

### Medical Records
| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/medical-records` | POST | Vet | Create medical record |
| `/api/v1/medical-records/{id}` | GET | Farmer/Vet | View record |
| `/api/v1/animals/{id}/medical-history` | GET | Farmer | Animal medical history |
| `/api/v1/appointments/{id}/medical-record` | GET | Farmer/Vet | Record by appointment |

### Dashboard
| Endpoint | Method | Role | Description |
|---|---|---|---|
| `/api/v1/dashboard` | GET | Authenticated | Role-aware dashboard metrics |

---

## 🏛️ Architecture

```
src/main/java/app/vetra/
├── auth/                    # Authentication module (JWT, refresh tokens, user management)
├── animal/                  # Animal management module
├── appointment/             # Appointment management + state machine
├── medicalrecord/           # Electronic Veterinary Medical Records (EVMR)
├── dashboard/               # Dashboard metrics aggregation
└── infrastructure/
    ├── persistence/
    │   ├── entity/          # JPA entities
    │   └── enums/           # Domain enumerations
    ├── security/            # Spring Security configuration, JWT filter
    └── config/              # JPA, application config
```

### Key Design Decisions

- **Medical records are immutable** — no PUT or DELETE endpoints. Clinical history is a permanent legal record.
- **Appointment state machine** — appointments progress through `PENDING → CONFIRMED → COMPLETED/CANCELLED` with optimistic locking.
- **Authenticated ownership** — the authenticated user's identity is always bound server-side. Client inputs cannot impersonate another user.
- **Single record per appointment** — duplicate creation returns `409 CONFLICT`.

---

## 🐳 Docker

### Development

```bash
docker compose -f docker-compose.dev.yml up -d
```

### Production

```bash
docker compose up -d
```

### Build Docker image

```bash
docker build -t vetra-backend .
```

---

## 📁 Folder Structure

```
vetra-backend/
├── docs/
│   ├── api/             # API documentation
│   ├── architecture/    # Architecture decisions
│   ├── database/        # ERD, schema notes
│   └── deployment/      # Docker & deployment guides
├── src/
│   ├── main/
│   │   ├── java/        # Application source
│   │   └── resources/
│   │       ├── db/migration/   # Flyway migrations
│   │       ├── application.yml
│   │       └── application-dev.yml
│   └── test/            # Unit & integration tests
├── Dockerfile
├── docker-compose.yml
├── docker-compose.dev.yml
├── pom.xml
├── checkstyle.xml
└── .env.example
```

---

## 🔗 Related Repository

- **Flutter Mobile App**: [github.com/omrajput14/vetra](https://github.com/omrajput14/vetra)

---

## 📄 License

To be determined.
