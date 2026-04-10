# Monorepo Restructure & Framework Sync

**Date:** 2026-04-10
**Status:** Approved
**Reference project:** `~/dev/bloc-portal`

## Goal

Restructure cyoda-test-management-system to match bloc-portal's monorepo layout and synchronize framework code, resources, and build configuration. This aligns both projects on the same Cyoda platform foundation, making future framework upgrades a simple copy.

## Approach

Single feature branch, one PR with 4 logical commits. Approach C from brainstorming: atomic merge with reviewable commit-by-commit history.

---

## Commit 1: Monorepo Restructure

### Directory Moves (git mv to preserve history)

- `backend/` -> `apps/backend/`
- `frontend/` -> `apps/frontend/`
- `backend/gradle/` -> `gradle/` (root level)
- `backend/gradlew` -> `gradlew` (root level)
- `backend/gradlew.bat` -> `gradlew.bat` (root level)
- `backend/helm/` -> `helm/` (root level)

### New Root Files

**`settings.gradle`:**
```gradle
rootProject.name = 'cyoda-test-management-system'
include ':apps:backend'

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

**`build.gradle`:**
```gradle
// Root build file -- dependency resolution is configured in settings.gradle.
// Subproject build logic lives in apps/backend/build.gradle.
```

**`nx.json`:**
```json
{
  "$schema": "https://raw.githubusercontent.com/nrwl/nx/master/packages/nx/schemas/nx-schema.json",
  "plugins": [
    {
      "plugin": "@nx/vite"
    }
  ],
  "defaultBase": "main",
  "useDaemonProcess": false
}
```

**`pnpm-workspace.yaml`:**
```yaml
packages:
  - 'apps/frontend'
```

**`package.json`:** Nx-based monorepo scripts adapted for TMS:
```json
{
  "name": "cyoda-test-management-system",
  "private": true,
  "scripts": {
    "dev:frontend": "nx dev frontend",
    "dev:backend": "nx runApp backend",
    "build": "nx run-many -t build --projects=frontend,backend",
    "test": "nx run-many -t test --projects=backend"
  },
  "devDependencies": {
    "@nx/vite": "^22.5.1",
    "nx": "^22.5.1"
  },
  "pnpm": {
    "onlyBuiltDependencies": ["@swc/core", "canvas", "esbuild", "nx"]
  }
}
```

**`.nxignore`:**
```
node_modules
build
.gradle
docs
```

**`.java-version`:**
```
21
```

**`apps/backend/project.json`:** Nx targets for Gradle tasks (build, test, runApp, bootRun, cucumberTest, runTestApp, validateWorkflowImplementations). All commands use `./gradlew :apps:backend:<task>` from workspace root.

### .gitignore Replacement

Replace current `.gitignore` with bloc-portal's version, adapted for TMS:
- `.gradle`, `.gradle-home/`
- `.env*` with `!.env.template` and `!.env.example`
- `**/build/` with `!src/**/build/`
- `node_modules/`, `.nx/`
- `apps/frontend/dist/`, `apps/frontend/.vite/`
- `/.idea/**`
- `.kotlin/sessions/`
- `.claude/worktrees`, `.claude/settings.local.json`
- `.sandbox/`
- `application-*.yml`
- `.DS_Store`

### Git Cleanup (remove from tracking)

Files currently committed that should not be:
- 18 files under `backend/.gradle/` (cache, checksums, locks, binaries)
- Root `.DS_Store`
- `frontend/bun.lock`, `frontend/bun.lockb`

Action: `git rm --cached` for all, then they're covered by the new `.gitignore`.

### Frontend Package Manager Switch

- Remove `bun.lock`, `bun.lockb`, `package-lock.json` from tracking
- Generate `pnpm-lock.yaml` via `pnpm install` in `apps/frontend/`
- Frontend continues to use npm scripts internally; pnpm manages the workspace

### CLAUDE.md Update

Update all paths in CLAUDE.md to reflect `apps/backend/` and root-level gradle:
- `cd backend && ./gradlew build` -> `./gradlew :apps:backend:build`
- `backend/src/...` paths -> `apps/backend/src/...`
- Project structure diagram updated

---

## Commit 2: Sync Framework Code

### `src/main/java/com/java_template/common/` -- Full Replacement

Delete TMS's current 47-file `common/` package and replace with bloc-portal's 121-class version, 1:1 identical. This brings:

- `auth/` -- OBO tokens, JWT signing, encryption (~22 classes)
- `config/` -- expanded with `CyodaLightConfigCustomizer`
- `controller/` -- `EntityCrudOperations`
- `dto/` -- `EntityWithMetadata`, `PageResult`
- `exception/` -- `CyodaOperationException`, `WorkflowExportException`
- `grpc/` -- full client infrastructure with connection management, reconnection, monitoring (~35 classes)
- `observability/` -- OpenTelemetry instrumentation (3 classes)
- `repository/` -- `CrudRepository`, `CyodaRepository`, `SearchAndRetrievalParams`
- `serializer/` -- full serialization framework including Jackson implementations
- `service/` -- `EntityService`, `WorkflowService`, `EdgeMessageService` + implementations
- `tool/` -- `CyodaInit`, `WorkflowImportTool`, validators
- `util/` -- `CyodaExceptionUtil`, `HttpUtils`, `JsonUtils`, `SslUtils`, HTTP parsing
- `workflow/` -- core abstractions (`CyodaEntity`, `CyodaProcessor`, `CyodaCriterion`, etc.) + ops

### `src/main/kotlin/org/cyoda/uuid/` -- Sync

Ensure 3 Kotlin files are 1:1 identical with bloc-portal.

### Test Sync

- `src/test/java/com/java_template/common/` -- replace with bloc-portal's common tests
- `src/test/java/com/example/` -- sync example entity tests from bloc-portal
- `src/test/kotlin/org/cyoda/uuid/` -- sync
- `src/test/resources/example/` -- sync example workflow configs

### Preserved (not touched)

- `src/test/java/com/java_template/application/` -- TMS application tests
- `src/test/java/e2e/` -- TMS E2E tests
- `src/test/resources/features/` -- TMS Gherkin features
- `src/test/resources/workflows/` -- TMS test workflows

---

## Commit 3: Sync Resources & Build Config

### New Resource Directories (1:1 from bloc-portal)

- `resources/api/` -- 6 OpenAPI specs:
  - `openapi.yml`
  - `openapi-audit.yml`
  - `openapi-common.yml`
  - `openapi-entity-search.yml`
  - `openapi-iam.yml`
  - `openapi-workflow.yml`
- `resources/META-INF/` -- Spring autoconfig:
  - `spring.factories`
  - `spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `resources/trino/` -- `trino-truststore.p12`

### Proto File Move

- Move `src/main/proto/cloudevents.proto` + `cyoda-cloud-api.proto` -> `src/main/resources/proto/`
- Delete `src/main/proto/` directory
- build.gradle `sourceSets.main.proto` configured to read from `resources/proto/`

### Replaced Resources

- `resources/logback.xml` -- bloc-portal's version (human-readable default + JSON/cloud profile via LogstashEncoder)
- `resources/applicationExample.yml` -- bloc-portal's version adapted (replace `bloc-portal` references with TMS naming, keep full structural template)

### Synced Resources

- `resources/schema/` -- sync to be 1:1 with bloc-portal's version

### Merged Resources

- `resources/application.yml` -- adopt bloc-portal's structural format, add missing sections (OBO, trino, observability, gRPC tuning) with placeholder values, preserve TMS-specific settings (port, entity names, auth config, CORS origins)

### Preserved Resources (TMS-specific)

- `resources/entity/` -- TMS entity schemas
- `resources/workflow/` -- TMS workflow JSONs
- `resources/functional_requirements/` -- TMS spec docs

### Dockerfile

Copy from bloc-portal, adapt:
- App paths reference `apps/backend/`
- App name references changed from `bloc-portal` to TMS

### Helm

Sync structure from bloc-portal's `helm/`:
- `Chart.yaml` -- adapt chart name for TMS
- `values.yaml` -- adapt for TMS
- `templates/` -- sync all templates

### `apps/backend/build.gradle` Alignment

Start from bloc-portal's version and adapt:

**Keep from bloc-portal (additions vs current TMS):**
- `org.openapi.generator` plugin (v7.12.0)
- OpenAPI codegen tasks (5 specs + aggregate task + JsonAlias fix)
- `otelAgent` configuration
- `io.opentelemetry:opentelemetry-api:1.44.1`
- `io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.11.0`
- `io.opentelemetry.javaagent:opentelemetry-javaagent:2.11.0` (otelAgent config)
- `net.logstash.logback:logstash-logback-encoder:8.0`
- `com.bucket4j:bucket4j-core:8.10.1`
- `spring-boot-starter-oauth2-resource-server`
- `io.opentelemetry:opentelemetry-sdk-testing:1.44.1` (test)
- `io.trino:trino-jdbc:479`
- `proto` sourceSet pointing to `src/main/resources/proto`
- OpenAPI generated sources in `sourceSets.main.java`
- `user.dir` system property override in test task
- `runTestApp`, `validateFunctionalRequirements`, `validateWorkflows`, `printOtelAgentPath` tasks

**Remove (riskblocs-specific):**
- `org.apache.pdfbox:pdfbox:3.0.3`

**Adapt references:**
- `com.riskblocs.Application` -> `com.java_template.Application` (bootJar, bootRun, runApp)
- `com.riskblocs.common.tool.*` -> `com.java_template.common.tool.*` (validator tasks, bootJarWorkflowImport)
- `com.riskblocs.application.testdata.*` -> remove or point to TMS equivalent

**Remove from current TMS build.gradle:**
- `repositories { mavenCentral() }` block -- now in root `settings.gradle`
- `spring-boot-devtools` dependency
- `archiveFileName.set("app.jar")` and `jar { enabled = false }` -- Dockerfile handles rename
- `systemProperty 'spring.config.import'` in cucumberTest

**Keep from current TMS build.gradle:**
- `org.awaitility:awaitility:4.2.2` test dependency (TMS E2E tests use it)

---

## Commit 4: Verify & Fix

- Run `./gradlew :apps:backend:compileJava :apps:backend:compileKotlin` -- confirm compilation
- Run `./gradlew :apps:backend:test` -- confirm unit tests pass
- Verify code generation tasks succeed: `generateOpenApi`, `generateProto`, `generateJsonSchema2Pojo`
- Fix any import/signature mismatches between synced `common/` and TMS `application/` code
- Fix any test failures caused by class changes

---

## Out of Scope

- TMS `application/` code (controllers, services, entities, processors, DTOs)
- TMS entity schemas and workflow configs
- TMS E2E tests and feature files
- TMS-specific documentation (functional requirements, test plans)
- Adding new TMS features or changing business logic
- E2E test execution (requires live Cyoda instance)
