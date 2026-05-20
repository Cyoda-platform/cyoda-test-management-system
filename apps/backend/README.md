# Backend — Technical Reference

> For first-time setup, user configuration, and running the app see the [root README](../../README.md).

## Commands

```bash
# Build (skip tests for speed)
./gradlew :apps:backend:build -x test -q

# Unit tests
./gradlew :apps:backend:test

# E2E / Cucumber tests (requires live Cyoda instance)
./gradlew :apps:backend:cucumberTest

# Run the backend
./gradlew :apps:backend:runApp

# Coverage report (Jacoco)
./gradlew :apps:backend:jacocoTestReport
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

**`common/`** — Cyoda framework: auth, gRPC client, EntityService, serializers. Never modify — changes break upgrades.

**`application/`** — all business logic goes here. Controllers, entities, processors, criteria, services.

---

## Core Concepts

**`CyodaEntity`** — domain objects. Implement in `application/entity/`. Must implement `getModelKey()` and `isValid()`.

**`CyodaProcessor`** — handle business logic during workflow transitions. Implement in `application/processor/`. Cannot call EntityService to update the entity currently being processed.

**`CyodaCriterion`** — evaluate transition conditions. Implement in `application/criterion/`. Must be pure functions — no side effects, no entity mutations.

**`EntityWithMetadata<T>`** — unified wrapper used across controllers, processors, and criteria. Technical entity ID comes from `entityWithMetadata.metadata().getId()`.

**`EntityService`** — single interface for all Cyoda data operations. Injected via constructor.

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

**Before re-importing**, delete all tenant entities and unlock/delete all models first — see [SCHEMA_AND_WORKFLOW_IMPORT.md](./SCHEMA_AND_WORKFLOW_IMPORT.md) for the exact deletion order. Skipping this causes orphaned entities and broken imports.

---

## Search API

**Global search** (across all projects):
```
GET /api/v1/search?query=login&pageNumber=0&pageSize=10
```

**Project-scoped search** (autocomplete, max 5 results):
```
GET /api/projects/{projectId}/search/quick?query=login
```

Searches across projects, suites, test cases, test runs, defects, and reports in parallel. Results ranked by relevance score.

---

## Further Reading

- [SCHEMA_AND_WORKFLOW_IMPORT.md](./SCHEMA_AND_WORKFLOW_IMPORT.md) — full import procedure
- [CYODA_INTEGRATION.md](./CYODA_INTEGRATION.md) — EntityService, gRPC, workflow configuration
- [CONTRIBUTING.md](./CONTRIBUTING.md) — development guidelines
- [usage-rules.md](./usage-rules.md) — implementation rules for processors, criteria, controllers
