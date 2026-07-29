# Developer Onboarding Guide
**Document ID:** GUIDE-20  
**Status:** Active  
**Last Updated:** 2026-07-29  
**Applies To:** All Vetra repositories  
**References:** [Engineering Principles](../engineering/00-principles.md), [Git Workflow](../engineering/13-git-workflow.md), [Environment Configuration](../operations/24-environment-config.md)

---

## Welcome to Vetra

Vetra is an AI-powered livestock health and disease surveillance platform built for rural farmers and field veterinarians. This guide will take you from zero to a running development environment in under 30 minutes.

---

## Prerequisites

Install these tools before proceeding:

| Tool | Version | Install |
|---|---|---|
| Java (JDK) | 17+ | `brew install --cask temurin@17` (macOS) |
| Maven | Included (use `./mvnw`) | Bundled in repo |
| Docker Desktop | Latest | https://www.docker.com/products/docker-desktop |
| Git | 2.40+ | `brew install git` |
| IntelliJ IDEA | Community or Ultimate | https://www.jetbrains.com/idea/ |
| Postman or HTTPie | Any | For API testing |

Verify installations:
```bash
java -version        # Should show 17.x
docker --version     # Should show 20+
git --version        # Should show 2.40+
```

---

## Repository Setup

### Step 1: Clone the Repositories

Vetra has two independent repositories. Clone both:

```bash
# Flutter client
git clone https://github.com/omrajput14/vetra.git ~/vetra

# Spring Boot backend
git clone https://github.com/omrajput14/vetra-backend.git ~/vetra-backend
```

### Step 2: Verify Repository Independence

```bash
cd ~/vetra && git remote -v
# Should show: origin → https://github.com/omrajput14/vetra.git

cd ~/vetra-backend && git remote -v
# Should show: origin → https://github.com/omrajput14/vetra-backend.git
```

---

## Backend Setup (Spring Boot)

### Step 1: Environment Configuration

```bash
cd ~/vetra-backend

# Copy the environment template
cp .env.example .env
```

Edit `.env` and set the `JWT_SECRET` to a secure random value:
```bash
openssl rand -base64 32
# Copy the output into JWT_SECRET in .env
```

### Step 2: Start PostgreSQL via Docker Compose

```bash
docker compose -f docker-compose.dev.yml up -d

# Verify it's running
docker compose -f docker-compose.dev.yml ps
# Expected: postgres container in "healthy" state
```

### Step 3: Run the Application

```bash
./mvnw spring-boot:run
```

Watch for this in the output:
```
Started VetraApplication in 3.2 seconds
```

### Step 4: Verify the API

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Step 5: Run the Tests

```bash
# Unit tests only (fast, ~30 seconds)
./mvnw test

# Full test suite (requires Docker for Testcontainers, ~2-3 minutes)
./mvnw verify
```

---

## Flutter Setup

### Prerequisites

| Tool | Version | Install |
|---|---|---|
| Flutter SDK | 3.x (stable) | https://docs.flutter.dev/get-started/install |
| Android Studio | Latest | https://developer.android.com/studio |
| Android SDK | API 34+ | Via Android Studio SDK Manager |
| Xcode | 15+ (macOS only) | Mac App Store |

Verify Flutter installation:
```bash
flutter doctor
# All items should show ✓ or have clear resolution instructions
```

### Step 1: Install Dependencies

```bash
cd ~/vetra
flutter pub get
```

### Step 2: Verify Configuration

Open `lib/core/config/app_config.dart` and confirm the base URL:
```dart
// For Android emulator connecting to local backend:
static const String baseUrl = 'http://10.0.2.2:8080';

// For physical device: change to your machine's local IP
// static const String baseUrl = 'http://192.168.1.x:8080';
```

### Step 3: Set Up an Emulator

In Android Studio:
1. `Tools → Device Manager → Create Device`
2. Select: Pixel 9 (or any Pixel model)
3. System image: Android 14 (API 34) — x86_64
4. Click Finish

Or from terminal:
```bash
emulator -list-avds
emulator -avd Pixel_9
```

### Step 4: Ensure Backend is Running

The Flutter app requires the backend to be running. Complete the backend setup above first.

### Step 5: Run the App

```bash
# List connected devices
flutter devices

# Run on emulator
flutter run -d emulator-5554

# Run on physical device (USB debugging enabled)
flutter run -d <device-id>
```

### Step 6: Verify the App

The splash screen should appear, followed by the onboarding/login screen. Create a test farmer account using the registration screen.

---

## Understanding the Codebase

### Backend Structure

```
src/main/java/app/vetra/
├── auth/             ← Register, Login, JWT, Refresh Token
├── animal/           ← Animal management for farmers
├── appointment/      ← Booking, state machine, vet scheduling
├── medicalrecord/    ← EVMR creation and retrieval
├── dashboard/        ← Role-specific dashboard aggregation
└── infrastructure/   ← JPA entities, security, config (shared)
```

### Flutter Structure

```
lib/
├── core/             ← Design system, router, network, config
├── features/
│   ├── auth/         ← Login, registration, token management
│   ├── farmer/       ← Farmer dashboard, animal list
│   ├── veterinarian/ ← Vet dashboard, appointment management
│   ├── animal/       ← Animal CRUD, QR code, passport
│   ├── appointment/  ← Booking flow, status updates
│   ├── medical_record/ ← EVMR creation (vet) and viewing (farmer)
│   └── ...
```

### Documentation Index

| Document | Location |
|---|---|
| Engineering Principles | `docs/engineering/00-principles.md` |
| Backend Architecture | `docs/architecture/02-SAD.md` |
| API Specification | `docs/api/06-specification.md` |
| Database Design | `docs/database/04-database-design.md` |
| Error Catalogue | `docs/api/23-error-catalogue.md` |
| Git Workflow | `docs/engineering/13-git-workflow.md` |

---

## Your First Task

Before writing any code:

1. Read the [Engineering Principles](../engineering/00-principles.md) — the engineering constitution of Vetra.
2. Read the [Git Workflow](../engineering/13-git-workflow.md) — understand how to branch and commit.
3. Explore the API with Postman:
   - `POST http://localhost:8080/api/v1/auth/register/farmer` — create a farmer account
   - `POST http://localhost:8080/api/v1/auth/login` — obtain a JWT
   - `GET http://localhost:8080/api/v1/dashboard` — check the dashboard response
4. Register both a farmer and a vet, and trace a full appointment booking flow.

---

## Getting Help

| Resource | Location |
|---|---|
| Architecture Decision Log | `docs/domain/21-decision-log.md` |
| Known issues and tradeoffs | Architecture Decision Records (`docs/architecture/adr/`) |
| Backend error codes | `docs/api/23-error-catalogue.md` |
| API reference | `docs/api/06-specification.md` |
