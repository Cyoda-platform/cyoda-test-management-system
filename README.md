# Cyoda Test Management System

A test management platform providing a complete lifecycle for projects, test suites, test cases, test runs, defect linking, and attachments. Built on the Cyoda platform for FSM-driven workflow entity management.

## Mono-repo structure

| Path | Description |
|------|-------------|
| `apps/backend/` | Java 21 / Spring Boot 3.x backend with Cyoda platform integration |
| `apps/frontend/` | React 18 / TypeScript / Vite frontend |
| `docs/` | Product specs, design docs, implementation plans |

This is an [Nx](https://nx.dev) mono-repo. Run tasks via `pnpm nx <target> <project>`.

## Prerequisites

- **Java 21** – [Install Java 21](https://adoptium.net/)
- **Node.js 18+** and **npm** (or **pnpm**)
- **Cyoda platform credentials** (optional for local dev — hardcoded credentials are used by default)
  - Get M2M credentials by logging into [ai.cyoda.net](https://ai.cyoda.net/) and prompting: *"Create a technical user for my environment \<env-name\>"*
  - Store in `.env` file (copy from `.env.example`)

## 🚀 Quick Start

### 1. Install Dependencies (First Time Only)

```bash
npm install
```

### 2. Start Backend + Frontend Together

```bash
./gradlew :apps:backend:build -x test -q && bash start-dev.sh
```

This script:
- Starts the **backend** on `http://localhost:8080/api`
- Starts the **frontend** on `http://localhost:5173`
- Uses hardcoded Cyoda credentials (or loads from `.env` if it exists)
- Waits for backend to be ready before starting frontend

Access the app:
- **Frontend**: [http://localhost:5173](http://localhost:5173)
- **Backend API Docs**: [http://localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html)
- **Default credentials**: `admin` / `admin123` (ADMIN role)

### 3. For Production: Set Cyoda Credentials

Copy `.env.example` to `.env` and fill in your Cyoda credentials:

```bash
cp .env.example .env
# Edit .env with your actual credentials
```

Then use `start-dev.sh` as usual — it will load credentials from `.env`.

## 🔧 Scripts

All developer scripts live in the project root:

| Script | Purpose |
|--------|---------|
| `start-dev.sh` | Start backend + frontend together (recommended for development) |
| `run-with-env.sh` | Start backend only, loads credentials from `.env` |
| `get-tokens.sh` | Get JWT tokens for `admin` and `tester` users (manual API testing) |
| `import-schemas.sh` | Import all entity schemas and workflows to Cyoda (see `apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md`) |
| `seed-demo.sh` | Seed demo data into a running instance |
| `e2e-snapshot-test.sh` | Run E2E snapshot tests |

## 📖 Per-App Documentation

For detailed information:
- **Frontend**: See [`apps/frontend/README.md`](apps/frontend/README.md)
- **Backend**: See [`apps/backend/README.md`](apps/backend/README.md)
- **Build & Test Commands**: See [`CLAUDE.md`](CLAUDE.md)

## Working with AI agents

This project uses **Claude Code** for agent-assisted development. Agent behavior is configured in:

| File | Scope |
|------|-------|
| [`CLAUDE.md`](CLAUDE.md) | Root — architecture, key references, general guidelines |

Human contributors should read CLAUDE.md before opening a PR.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for workflow guidelines, branch conventions, and PR expectations.
