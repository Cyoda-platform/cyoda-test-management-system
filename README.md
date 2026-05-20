# Cyoda Test Management System

A test management platform for projects, test suites, test cases, test runs, defect tracking, and reports.

Built on **[Cyoda](https://cyoda.io)** — a workflow engine that manages entity lifecycles through finite-state machines (FSM). Cyoda stores all data and drives state transitions; this app provides the API and UI on top of it.

## Mono-repo structure

| Path | Description |
|------|-------------|
| `apps/backend/` | Java 21 / Spring Boot 3.5 backend |
| `apps/frontend/` | React 18 / TypeScript / Vite frontend |
| `docs/` | Product specs, design docs |

## Prerequisites

- **Java 21** — [Install Java 21](https://adoptium.net/)
- **Node.js 18+** — [Install Node.js](https://nodejs.org/)
- **Cyoda instance** — get M2M credentials from [Cyoda AI Studio](https://studio.cyoda.io)
  - Prompt: *"Create a technical user for my environment \<env-name\>"*

> **Frontend package manager:** the workspace is configured for **pnpm**, but plain `npm` works too. Pick one and stick with it.

## First-time setup

### 1. Install dependencies

```bash
npm install
```

### 2. Configure environment

```bash
cp .env.example .env
```

Edit `.env` and fill in:
- `CYODA_HOST`, `CYODA_CLIENT_ID`, `CYODA_CLIENT_SECRET` — from Cyoda AI Studio
- `APP_USERS_0_*`, `APP_USERS_1_*` — your user accounts and passwords (see [User accounts](#user-accounts) below)

The app **will not start** if user passwords are blank.

### 3. Import schemas and workflows

Required once after setting up a fresh Cyoda instance:

```bash
./import-schemas.sh
```

See [`apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md`](apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md) for details.

### 4. Start the app

```bash
./gradlew :apps:backend:build -x test -q && bash start-dev.sh
```

- **Frontend**: [http://localhost:5173](http://localhost:5173)
- **Backend API docs**: [http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)

---

## User accounts

Users are defined in `.env` — there are no built-in accounts. Add one block per user:

```bash
APP_USERS_0_USERNAME=admin
APP_USERS_0_PASSWORD=your-strong-password
APP_USERS_0_ROLE=ADMIN

APP_USERS_1_USERNAME=tester
APP_USERS_1_PASSWORD=another-password
APP_USERS_1_ROLE=TESTER
```

- `ADMIN` — full access (projects, suites, test cases, runs, reports)
- `TESTER` — can execute runs and log defects; cannot create or modify projects/suites/cases
- Any number of users; multiple testers and multiple admins are supported

---

## Scripts

| Script | Purpose |
|--------|---------|
| `start-dev.sh` | Start backend + frontend together |
| `run-with-env.sh` | Start backend only |
| `get-tokens.sh` | Get JWT tokens for API testing (reads credentials from `.env`) |
| `import-schemas.sh` | Import entity schemas and workflows to Cyoda |
| `seed-demo.sh` | Seed demo data into a running instance |

---

## Documentation

- **Backend**: [`apps/backend/README.md`](apps/backend/README.md) — commands, project structure, core concepts
- **Frontend**: [`apps/frontend/README.md`](apps/frontend/README.md)
- **Workflow import**: [`apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md`](apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md)
- **Contributing**: [`CONTRIBUTING.md`](CONTRIBUTING.md)

## Working with AI agents

Agent behavior is configured in [`CLAUDE.md`](CLAUDE.md). Read it before opening a PR.
