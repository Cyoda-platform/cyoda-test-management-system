# Cyoda TMS

A **Java 21 + Kotlin / Spring Boot 3.5 / Gradle** test-management platform built on the Cyoda workflow engine.
Goal: provide a complete lifecycle for projects, test suites, test cases, test runs, defect linking, and attachments — backed by Cyoda's FSM-driven entity service.

---

## Development Gates

These are **STOP-and-verify** checkpoints. Do not proceed past a gate without completing it.

### Gate 1: TDD is mandatory

Do not write implementation code without a failing test driving it.
Use `superpowers:test-driven-development` skill for all feature and bugfix work.
See `.claude/rules/tdd.md` for the full protocol if present.

### Gate 2: E2E test coverage

When adding or changing user-facing behaviour (API responses, workflow semantics, error codes), add or update E2E tests.

- **Cucumber/Gherkin E2E tests** live in `apps/backend/src/test/java/e2e/` with feature files under `apps/backend/src/test/resources/features/`.
- E2E tests require a **live Cyoda instance** (not Docker). Configure via `apps/backend/src/test/resources/application-cucumber.yaml` or the env vars listed in `.env.example`.
- Run with: `./gradlew :apps:backend:cucumberTest`
- Coverage report is generated automatically by Jacoco: `build/test-report/`.

### Gate 3: Security by default

- Never log credentials, tokens, client secrets, or signing keys at any log level.
- Every data path must respect **tenant isolation** — no cross-tenant leakage through `EntityService` calls.
- Validate input at controller boundaries (Bean Validation / `@Valid`).
- Sanitize API responses — no stack traces or internal details returned to callers. Use `CyodaExceptionUtil` for structured error responses.
- `ssl-trust-all: true` is a test-only flag — never enable it in production profiles.

### Gate 4: Documentation hygiene

- When changing env vars, update `.env.example`, the relevant `application*.yml`, and `apps/backend/README.md` together.
- When changing public API behaviour or developer workflow, update `apps/backend/README.md`, `apps/backend/CONTRIBUTING.md`, and `apps/backend/usage-rules.md`.
- When changing `common/` interfaces (framework code), update `apps/backend/llms.txt` and `apps/backend/llms-full.txt`.

### Gate 5: Verify before claiming done

Use `superpowers:verification-before-completion` skill before claiming work is complete.

```bash
# Unit tests only (fast, no Cyoda instance required)
./gradlew :apps:backend:test

# E2E / Cucumber tests (requires live Cyoda instance)
./gradlew :apps:backend:cucumberTest

# Full build + compile check
./gradlew :apps:backend:build

# Static analysis (compiler warnings treated strictly)
./gradlew :apps:backend:compileJava :apps:backend:compileKotlin
```

Do **not** claim work is done if any test — unit, integration, or E2E — is failing.

---

## Tech Stack & Conventions

### Language & Runtime

- **Java 21** (primary application language) + **Kotlin 2.2.21** (utility/UUID code in `src/main/kotlin/`).
- **Spring Boot 3.5.3** — web, validation, OAuth2 client, actuator, devtools.
- **Gradle** wrapper (`./gradlew`) — never invoke `gradle` directly.
- Mixed Java/Kotlin sources compile together; both `src/main/java/` and `src/main/kotlin/` are in the `main` source set.

### Logging

- Use **SLF4J** exclusively. In Java classes annotate with Lombok `@Slf4j` and call `log.info(...)`. In Kotlin use `private val log = LoggerFactory.getLogger(...)`.
- **Never** use `System.out.println`, `System.err.println`, or raw `java.util.logging`.
- Structured log fields preferred over string concatenation.

### Dependency Injection

- Spring `@Component` / `@Service` / `@Repository` + constructor injection.
- **No field injection** (`@Autowired` on fields). Constructor injection only.
- No reflection-based DI tricks; framework auto-discovery via `@Component` scan.

### Error Handling

- Wrap and re-throw with context: `throw new RuntimeException("Failed to X: " + e.getMessage(), e)` or a domain-specific exception.
- 4xx responses: return full domain detail with a structured error body (use `CyodaExceptionUtil`).
- 5xx responses: return a generic message + a correlation/ticket UUID — never expose stack traces.
- Use `@ControllerAdvice` for global exception mapping.

### UUIDs

- Use `java.util.UUID` — never plain `String` for entity identifiers.
- Entity technical IDs come from `entityWithMetadata.metadata().getId()`.

### Configuration

- Config via Spring Boot YAML (`application.yml`) with env-var overrides.
- All Cyoda-specific settings are under the `app.config.*` namespace (see `.env.example`).
- Key env vars: `CYODA_HOST`, `CYODA_CLIENT_ID`, `CYODA_CLIENT_SECRET`, `SERVER_PORT`, `SPRING_PROFILES_ACTIVE`, `CORS_ALLOWED_ORIGINS`, `LOG_LEVEL`.
- Never commit `.env` — it is gitignored. Always keep `.env.example` up to date.

### Framework Code (`common/`) — DO NOT MODIFY

`apps/backend/src/main/java/com/java_template/common/` is framework code shared with the Cyoda template.
**Never modify anything in `common/`.** Changes there break upgrades and violate the template contract.
Business logic belongs exclusively in `application/`.

### Entity / Processor / Criterion Rules

- **Entities** implement `CyodaEntity`; placed in `application/entity/`.
- **Processors** implement `CyodaProcessor`; placed in `application/processor/`. A processor **cannot** call `EntityService` to update the entity currently being processed.
- **Criteria** implement `CyodaCriterion`; placed in `application/criterion/`. Must be **pure functions** — no side effects, no entity mutations.
- **Controllers**: never accept `Map<String, Object>` bodies; never return raw `Object`; always use `EntityWithMetadata<T>`.
- Use `SerializerFactory` → `ProcessorSerializer` / `CriterionSerializer` — never raw `ObjectMapper` in processors.

### Workflow Configurations

- FSM JSON files go in `apps/backend/src/main/resources/workflow/$entity_name/version_$version/$entity_name.json`.
- **Do NOT use `WorkflowImportTool`** — it is superseded by the HTTP curl import script.
- Import schemas + workflows with `./import-schemas.sh` (run from `apps/backend/`).
- **Before re-importing**, you MUST delete all tenant entities (bottom-up) and unlock/delete all models first. See **`apps/backend/SCHEMA_AND_WORKFLOW_IMPORT.md`** for the exact deletion order and commands. Skipping this causes orphaned entities and broken imports.
- Avoid cyclic FSM states. Processor/criterion `supports()` return values must match the operation names in the JSON.

### Frontend (`apps/frontend/`)

- React 18 + TypeScript + Vite. Package manager: `pnpm`.
- Unit tests: `vitest` — `npm run test`.
- E2E browser tests: Playwright — `npx playwright test`.
- Build: `npm run build`.
- Dev server: `npm run dev` (default port 5173; backend CORS allows this origin).

---

## Workflow

### New Feature

```
brainstorming → writing-plans → subagent-driven-development → verification-before-completion → requesting-code-review → security-review → PR/merge
```

### Bugfix

```
test-driven-development → verification-before-completion → requesting-code-review → security-review → PR/merge
```

### Receiving Review Feedback

```
receiving-code-review
```

All workflow skills are in the `superpowers:` namespace.
Security review uses `antigravity-bundle-security-developer:cc-skill-security-review`.

**Do not skip steps.** Brainstorming prevents building the wrong thing. TDD prevents shipping untested code. Verification prevents false "done" claims. Review and security audit prevent defects reaching `main`.

---

## Common Commands

### Backend

| Task | Command |
|---|---|
| Build (compile + unit test) | `./gradlew :apps:backend:build` |
| Unit tests only | `./gradlew :apps:backend:test` |
| E2E / Cucumber tests | `./gradlew :apps:backend:cucumberTest` |
| Coverage report (Jacoco) | `./gradlew :apps:backend:jacocoTestReport` |
| Run application | `./gradlew :apps:backend:runApp` |
| Import workflows | `./gradlew :apps:backend:runApp -PmainClass=com.java_template.common.tool.WorkflowImportTool --args='--spring.profiles.active=local'` |
| Build fat JAR | `./gradlew :apps:backend:bootJar` |
| Validate workflow impls | `./gradlew :apps:backend:validateWorkflowImplementations` |
| Dependency refresh | `./gradlew :apps:backend:dependencies` |

### Frontend

| Task | Command |
|---|---|
| Dev server | `cd apps/frontend && npm run dev` |
| Build | `cd apps/frontend && npm run build` |
| Unit tests (vitest) | `cd apps/frontend && npm run test` |
| Lint | `cd apps/frontend && npm run lint` |

### Deferred Work

Mark deferred items in code as:
```java
// TODO(plan-reference): description
```

Search for all TODOs:
```bash
grep -r "TODO(plan-reference)" apps/backend/src/ apps/frontend/src/
```

---

## Project Structure (Quick Reference)

```
cyoda-test-management-system/
├── apps/
│   ├── backend/
│   │   ├── src/main/java/com/java_template/
│   │   │   ├── common/           # Framework code — DO NOT MODIFY
│   │   │   └── application/      # Business logic — implement here
│   │   │       ├── controller/
│   │   │       ├── entity/
│   │   │       ├── processor/
│   │   │       ├── criterion/
│   │   │       └── service/
│   │   ├── src/main/kotlin/      # Kotlin utilities (UUID, etc.)
│   │   ├── src/main/resources/
│   │   │   ├── application.yml
│   │   │   └── workflow/         # FSM workflow JSON configs
│   │   ├── src/test/java/
│   │   │   ├── e2e/              # Cucumber E2E tests (live Cyoda required)
│   │   │   └── com/java_template/ # Unit & integration tests
│   │   ├── src/test/resources/
│   │   │   ├── features/         # Gherkin feature files
│   │   │   └── application-cucumber.yaml
│   │   ├── llm_example/          # Reference patterns — always consult before implementing
│   │   ├── project.json          # Nx project configuration
│   │   ├── build.gradle
│   │   └── README.md
│   └── frontend/                 # React + TypeScript + Vite
├── gradle/wrapper/               # Gradle wrapper (root level)
├── helm/                         # Helm charts (root level)
├── docs/
├── settings.gradle               # Monorepo Gradle settings
├── build.gradle                  # Root build file
├── nx.json                       # Nx monorepo orchestration
├── package.json                  # Root package.json with Nx scripts
├── pnpm-workspace.yaml           # pnpm workspace config
├── .env.example                  # Required env var reference — keep up to date
└── CLAUDE.md                     # This file
```
