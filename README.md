# Cyoda Test Management System

A test management platform providing a complete lifecycle for projects, test suites, test cases, test runs, defect linking, and attachments. Built on the Cyoda platform for FSM-driven workflow entity management.

## Mono-repo structure

| Path | Description |
|------|-------------|
| `apps/backend/` | Java 21 / Spring Boot 3.x backend with Cyoda platform integration |
| `apps/frontend/` | React 18 / TypeScript / Vite frontend |
| `docs/` | Product specs, design docs, implementation plans |

This is an [Nx](https://nx.dev) mono-repo. Run tasks via `pnpm nx <target> <project>`.

## Prerequisites & getting started

- **Java 21**
- **Node.js 18+** and **pnpm**
- **Cyoda platform credentials** — log in to [ai.cyoda.net](https://ai.cyoda.net/) and prompt: *"Create a technical user for my environment \<env-name\>"*

```bash
# 1. Install JS dependencies
pnpm install

# 2. Set Cyoda credentials (required before starting the backend)
export APP_CONFIG_CYODA_HOST=your-env.cyoda.net
export APP_CONFIG_CYODA_CLIENT_ID=your-client-id
export APP_CONFIG_CYODA_CLIENT_SECRET=your-client-secret

# 3. Import workflows into Cyoda (first-time setup)
./gradlew :apps:backend:runApp -PmainClass=com.java_template.common.tool.WorkflowImportTool

# 4. Start the backend (port 8080, context path /api)
./gradlew :apps:backend:runApp

# 5. Start the frontend (port 5173)
pnpm nx serve frontend
```

Swagger UI is available at [localhost:8080/api/swagger-ui/index.html](http://localhost:8080/api/swagger-ui/index.html) once the backend is running.

**Default login credentials** (hardcoded for local dev):

| Username | Password | Role |
|----------|----------|------|
| `admin` | `admin123` | ADMIN |
| `tester` | `tester123` | TESTER |

See per-app CLAUDE.md files for the full build and test command reference.

## Working with AI agents

This project uses **Claude Code** for agent-assisted development. Agent behavior is configured in:

| File | Scope |
|------|-------|
| [`CLAUDE.md`](CLAUDE.md) | Root — architecture, key references, general guidelines |
| [`apps/backend/CLAUDE.md`](apps/backend/CLAUDE.md) | Backend build commands, patterns, conventions |
| [`.claude/rules/`](.claude/rules/) | Coding standards and per-layer rules |

Human contributors should read CLAUDE.md before opening a PR.

## CI/CD

GitHub Actions workflows live in [`.github/workflows/`](.github/workflows/). The primary pipeline is `build.yml`, which runs on pull requests.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for workflow guidelines, branch conventions, and PR expectations.
