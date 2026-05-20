# Backend — Cyoda Test Management System

A **Spring Boot 3.5 / Java 21** backend for managing test projects, suites, test cases, runs, defects, and reports — built on the Cyoda workflow engine.

---

## Prerequisites

- **Java 21** — [Install Java 21](https://adoptium.net/)
- **Cyoda instance** — get M2M credentials from [Cyoda AI Studio](https://studio.cyoda.io)

---

## First Deploy Setup

Before starting the app, create a `.env` file from the template:

```bash
cp .env.example .env
```

Then fill in the required values in `.env`:

1. **Cyoda credentials** — `CYODA_HOST`, `CYODA_CLIENT_ID`, `CYODA_CLIENT_SECRET`
2. **User accounts** — at least one user with a password (see [User Configuration](#-user-configuration) below)

The app will refuse to start if user passwords are blank.

---

## Quick Start

```bash
# From the project root
./gradlew :apps:backend:build -x test -q && bash start-dev.sh
```

This starts the backend on `http://localhost:8080/api`. The `.env` file is loaded automatically.

---

## 👤 User Configuration

Users are defined via environment variables — there are no built-in accounts. Add one block per user, incrementing the index (`0`, `1`, `2`, ...):

```bash
APP_USERS_0_USERNAME=admin
APP_USERS_0_PASSWORD=your-strong-password
APP_USERS_0_ROLE=ADMIN

APP_USERS_1_USERNAME=tester
APP_USERS_1_PASSWORD=another-password
APP_USERS_1_ROLE=TESTER

# Add more users by incrementing the index
APP_USERS_2_USERNAME=tester2
APP_USERS_2_PASSWORD=...
APP_USERS_2_ROLE=TESTER
```

- `ROLE` must be exactly `ADMIN` or `TESTER`
- `ADMIN` — full access (create/edit/delete projects, suites, test cases)
- `TESTER` — can execute test runs and log defects; cannot modify projects or test cases
- Any number of users is supported; multiple admins and multiple testers are allowed

---

## Commands

```bash
# Build (skip tests for speed)
./gradlew :apps:backend:build -x test -q

# Unit tests
./gradlew :apps:backend:test

# E2E / Cucumber tests (requires live Cyoda instance)
./gradlew :apps:backend:cucumberTest

# Run the backend directly
./gradlew :apps:backend:runApp
```

---

## Project Structure

```
src/main/java/com/java_template/
├── common/        # Framework code — DO NOT MODIFY
└── application/   # Business logic — implement here
    ├── controller/
    ├── entity/
    ├── processor/
    ├── criterion/
    └── service/
```

**`common/`** — Cyoda framework: auth, gRPC client, EntityService, serializers. Never modify.

**`application/`** — your business logic. All new features go here.

---

## Core Concepts

**`CyodaEntity`** — domain objects (implement in `application/entity/`).

**`CyodaProcessor`** — workflow components that handle business logic during transitions. Cannot call EntityService to update the entity currently being processed.

**`CyodaCriterion`** — pure functions that evaluate transition conditions. No side effects, no entity mutations.

**`EntityWithMetadata<T>`** — unified wrapper used across controllers, processors, and criteria. Entity technical ID comes from `entityWithMetadata.metadata().getId()`.

---

## Workflow Configuration

FSM workflow JSON files go in:
```
src/main/resources/workflow/$entity_name/version_$version/$entity_name.json
```

Import schemas and workflows with:
```bash
./import-schemas.sh
```

See **[SCHEMA_AND_WORKFLOW_IMPORT.md](./SCHEMA_AND_WORKFLOW_IMPORT.md)** for the full import procedure including the required deletion order before re-importing.

---

## Search API

The backend includes a unified search engine across all entity types.

**Global search** (header search bar):
```
GET /api/v1/search?query=login&pageNumber=0&pageSize=10
```

**Project-scoped search** (autocomplete, max 5 results):
```
GET /api/projects/{projectId}/search/quick?query=login
```

Searches across projects, suites, test cases, test runs, defects, and reports in parallel. Results are ranked by relevance score.

---

## Further Reading

- **[SCHEMA_AND_WORKFLOW_IMPORT.md](./SCHEMA_AND_WORKFLOW_IMPORT.md)** — importing workflows and schemas into Cyoda
- **[CYODA_INTEGRATION.md](./CYODA_INTEGRATION.md)** — EntityService, gRPC, and workflow configuration details
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — development guidelines
- **[usage-rules.md](./usage-rules.md)** — implementation rules for processors, criteria, and controllers
