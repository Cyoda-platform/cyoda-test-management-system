# Monorepo Restructure & Framework Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure cyoda-test-management-system to match bloc-portal's monorepo layout and synchronize framework code, resources, and build configuration 1:1.

**Architecture:** Four-commit approach on a single feature branch. Commit 1 restructures directories. Commit 2 syncs framework source code. Commit 3 syncs resources and build config. Commit 4 verifies compilation and tests.

**Tech Stack:** Java 21, Kotlin 2.2.21, Spring Boot 3.5.3, Gradle 8.7, Nx 22.5.1, pnpm

**Source of truth:** `~/dev/bloc-portal` (referred to as `$BP` below). Target: `~/dev/cyoda-test-management-system` (referred to as `$TMS`).

---

## Task 1: Create Feature Branch

**Files:** None (git operation only)

- [ ] **Step 1: Create and switch to feature branch**

```bash
cd /Users/paul/dev/cyoda-test-management-system
git checkout -b refactor/monorepo-restructure
```

- [ ] **Step 2: Verify clean state**

```bash
git status
```

Expected: On branch `refactor/monorepo-restructure`, clean working tree (`.sandbox/` is untracked, that's fine).

---

## Task 2: Git Cleanup — Remove Files That Should Not Be Tracked

**Files:**
- Remove from tracking: `backend/.gradle/**`, `.DS_Store`, `frontend/bun.lock`, `frontend/bun.lockb`

- [ ] **Step 1: Remove .gradle cache files from git tracking**

```bash
git rm -r --cached backend/.gradle/
```

- [ ] **Step 2: Remove .DS_Store from git tracking**

```bash
git rm --cached .DS_Store
```

- [ ] **Step 3: Remove bun lockfiles from git tracking**

```bash
git rm --cached frontend/bun.lock frontend/bun.lockb
```

- [ ] **Step 4: Verify removals are staged**

```bash
git status
```

Expected: All removed files show as "deleted" in staged changes, but files still exist on disk.

---

## Task 3: Replace .gitignore

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: Replace .gitignore with bloc-portal-aligned version**

Write the following to `.gitignore`:

```gitignore
# macOS
.DS_Store

.gradle
.gradle-home/

.env*
!.env.template
!.env.example

**/build/
!src/**/build/

# Ignore Gradle GUI config
gradle-app.setting

# Avoid ignoring Gradle wrapper jar file (.jar files are usually ignored)
!gradle-wrapper.jar

# Avoid ignore Gradle wrappper properties
!gradle-wrapper.properties

# Cache of project
.gradletasknamecache

# Eclipse Gradle plugin generated files
# Eclipse Core
.project
# JDT-specific (Eclipse Java Development Tools)
.classpath

/.idea/**

# Node / pnpm
node_modules/
pnpm-debug.log*

# Nx
.nx/

# Frontend build output
apps/frontend/dist/

apps/frontend/.vite/

.playwright-mcp/
.kotlin/sessions/

.claude/worktrees
.claude/settings.local.json

# agent-safehouse local settings
.sandbox/

application-*.yml
```

- [ ] **Step 2: Stage .gitignore**

```bash
git add .gitignore
```

---

## Task 4: Move Directories to Monorepo Layout

**Files:**
- Move: `backend/` -> `apps/backend/`
- Move: `frontend/` -> `apps/frontend/`
- Move: `backend/gradle/` -> `gradle/` (root)
- Move: `backend/gradlew` -> `gradlew` (root)
- Move: `backend/gradlew.bat` -> `gradlew.bat` (root)
- Move: `backend/helm/` -> `helm/` (root)

- [ ] **Step 1: Create apps directory**

```bash
mkdir -p apps
```

- [ ] **Step 2: Move backend to apps/backend**

```bash
git mv backend apps/backend
```

- [ ] **Step 3: Move frontend to apps/frontend**

```bash
git mv frontend apps/frontend
```

- [ ] **Step 4: Move gradle wrapper to root**

```bash
git mv apps/backend/gradle ./gradle
git mv apps/backend/gradlew ./gradlew
git mv apps/backend/gradlew.bat ./gradlew.bat
```

- [ ] **Step 5: Move helm to root**

```bash
git mv apps/backend/helm ./helm
```

- [ ] **Step 6: Ensure gradlew is executable**

```bash
chmod +x gradlew
```

- [ ] **Step 7: Verify structure**

```bash
ls -la apps/
ls -la gradle/wrapper/
ls -la gradlew
ls -la helm/
```

Expected: `apps/backend/`, `apps/frontend/` exist. `gradle/wrapper/` at root. `gradlew` at root and executable. `helm/` at root.

---

## Task 5: Add Root Monorepo Configuration Files

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `nx.json`
- Create: `pnpm-workspace.yaml`
- Create: `.nxignore`
- Create: `.java-version`
- Create: `apps/backend/project.json`
- Modify: `package.json`

- [ ] **Step 1: Create root settings.gradle**

```gradle
rootProject.name = 'cyoda-test-management-system'
include ':apps:backend'

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
```

- [ ] **Step 2: Create root build.gradle**

```gradle
// Root build file — dependency resolution is configured in settings.gradle.
// Subproject build logic lives in apps/backend/build.gradle.
```

- [ ] **Step 3: Create nx.json**

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

- [ ] **Step 4: Create pnpm-workspace.yaml**

```yaml
packages:
  - 'apps/frontend'
```

- [ ] **Step 5: Create .nxignore**

```
node_modules
build
.gradle
docs
```

- [ ] **Step 6: Create .java-version**

```
21
```

- [ ] **Step 7: Create apps/backend/project.json**

```json
{
  "name": "backend",
  "targets": {
    "build": {
      "executor": "nx:run-commands",
      "cache": true,
      "inputs": [
        "{projectRoot}/src/**",
        "{projectRoot}/build.gradle",
        "{projectRoot}/lombok.config",
        "{workspaceRoot}/settings.gradle",
        "{workspaceRoot}/gradle/**",
        "{workspaceRoot}/gradlew"
      ],
      "outputs": ["{projectRoot}/build/libs"],
      "options": {
        "command": "./gradlew :apps:backend:build",
        "cwd": "{workspaceRoot}"
      }
    },
    "test": {
      "executor": "nx:run-commands",
      "cache": true,
      "inputs": [
        "{projectRoot}/src/**",
        "{projectRoot}/build.gradle",
        "{workspaceRoot}/settings.gradle"
      ],
      "outputs": ["{projectRoot}/build/reports/tests"],
      "options": {
        "command": "./gradlew :apps:backend:test",
        "cwd": "{workspaceRoot}"
      }
    },
    "runApp": {
      "executor": "nx:run-commands",
      "cache": false,
      "continuous": true,
      "options": {
        "command": "./gradlew :apps:backend:runApp",
        "cwd": "{workspaceRoot}"
      }
    },
    "bootRun": {
      "executor": "nx:run-commands",
      "cache": false,
      "continuous": true,
      "options": {
        "command": "./gradlew :apps:backend:bootRun",
        "cwd": "{workspaceRoot}"
      }
    },
    "cucumberTest": {
      "executor": "nx:run-commands",
      "cache": false,
      "options": {
        "command": "./gradlew :apps:backend:cucumberTest",
        "cwd": "{workspaceRoot}"
      }
    },
    "runTestApp": {
      "executor": "nx:run-commands",
      "cache": false,
      "options": {
        "command": "./gradlew :apps:backend:runTestApp",
        "cwd": "{workspaceRoot}"
      }
    },
    "validateWorkflowImplementations": {
      "executor": "nx:run-commands",
      "cache": false,
      "options": {
        "command": "./gradlew :apps:backend:validateWorkflowImplementations",
        "cwd": "{workspaceRoot}"
      }
    }
  }
}
```

- [ ] **Step 8: Replace root package.json**

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

- [ ] **Step 9: Install pnpm dependencies at root**

```bash
pnpm install
```

- [ ] **Step 10: Install pnpm dependencies for frontend**

```bash
cd apps/frontend && pnpm install && cd ../..
```

- [ ] **Step 11: Remove old frontend lockfiles from disk (already removed from git in Task 2)**

```bash
rm -f apps/frontend/bun.lock apps/frontend/bun.lockb apps/frontend/package-lock.json
```

- [ ] **Step 12: Stage all new and changed files**

```bash
git add settings.gradle build.gradle nx.json pnpm-workspace.yaml .nxignore .java-version
git add apps/backend/project.json package.json pnpm-lock.yaml
git add apps/frontend/pnpm-lock.yaml 2>/dev/null || true
```

---

## Task 6: Update CLAUDE.md Paths

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update all path references in CLAUDE.md**

Replace all occurrences:
- `backend/src/` -> `apps/backend/src/`
- `backend/README.md` -> `apps/backend/README.md`
- `backend/CONTRIBUTING.md` -> `apps/backend/CONTRIBUTING.md`
- `backend/usage-rules.md` -> `apps/backend/usage-rules.md`
- `backend/llms.txt` -> `apps/backend/llms.txt`
- `backend/llms-full.txt` -> `apps/backend/llms-full.txt`
- `frontend/` -> `apps/frontend/` (in path references)
- `cd backend && ./gradlew` -> `./gradlew :apps:backend:` (for all command examples)
- `cd frontend && ` -> `cd apps/frontend && ` (for frontend commands)

Update the project structure diagram to reflect:
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
│   │   │   ├── api/              # OpenAPI specs
│   │   │   ├── application.yml
│   │   │   ├── logback.xml
│   │   │   ├── proto/            # Protobuf definitions
│   │   │   ├── schema/           # JSON schemas
│   │   │   └── workflow/         # FSM workflow JSON configs
│   │   ├── src/test/java/
│   │   │   ├── e2e/              # Cucumber E2E tests
│   │   │   └── com/java_template/ # Unit & integration tests
│   │   ├── src/test/resources/
│   │   │   ├── features/         # Gherkin feature files
│   │   │   └── application-cucumber.yaml
│   │   ├── build.gradle
│   │   └── README.md
│   └── frontend/                 # React + TypeScript + Vite
├── gradle/wrapper/               # Gradle wrapper (root level)
├── helm/                         # Kubernetes Helm charts
├── docs/
├── settings.gradle               # Monorepo module config
├── build.gradle                  # Root build file
├── nx.json                       # Nx monorepo config
├── package.json                  # Nx-based scripts
├── pnpm-workspace.yaml           # pnpm workspace
├── .env.example                  # Required env var reference
└── CLAUDE.md                     # This file
```

Update the Common Commands table:

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
| Frontend dev server | `cd apps/frontend && pnpm dev` |
| Frontend build | `cd apps/frontend && pnpm build` |
| Frontend unit tests | `cd apps/frontend && pnpm test` |
| Frontend lint | `cd apps/frontend && pnpm lint` |

- [ ] **Step 2: Stage CLAUDE.md**

```bash
git add CLAUDE.md
```

---

## Task 7: Commit 1 — Monorepo Restructure

- [ ] **Step 1: Review staged changes**

```bash
git status
git diff --cached --stat
```

Expected: Directory moves, new root config files, .gitignore replacement, git cleanup removals, CLAUDE.md updates.

- [ ] **Step 2: Commit**

```bash
git commit -m "refactor: restructure as monorepo matching bloc-portal layout

Move backend/ and frontend/ under apps/, gradle wrapper and helm to root.
Add Nx monorepo orchestration with pnpm workspace.
Replace .gitignore. Remove tracked .gradle cache files and .DS_Store.
Update CLAUDE.md paths to reflect new structure.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 8: Sync common/ Source Code

**Files:**
- Delete and replace: `apps/backend/src/main/java/com/java_template/common/` (entire directory)

- [ ] **Step 1: Delete current TMS common/ directory**

```bash
rm -rf apps/backend/src/main/java/com/java_template/common/
```

- [ ] **Step 2: Copy bloc-portal common/ directory**

```bash
cp -R ~/dev/bloc-portal/apps/backend/src/main/java/com/java_template/common/ \
      apps/backend/src/main/java/com/java_template/common/
```

- [ ] **Step 3: Verify file count matches bloc-portal**

```bash
find apps/backend/src/main/java/com/java_template/common/ -name "*.java" | wc -l
find ~/dev/bloc-portal/apps/backend/src/main/java/com/java_template/common/ -name "*.java" | wc -l
```

Expected: Both counts are identical.

- [ ] **Step 4: Verify 1:1 identical content**

```bash
diff -rq apps/backend/src/main/java/com/java_template/common/ \
         ~/dev/bloc-portal/apps/backend/src/main/java/com/java_template/common/
```

Expected: No output (no differences).

- [ ] **Step 5: Stage common/ changes**

```bash
git add apps/backend/src/main/java/com/java_template/common/
```

---

## Task 9: Sync Kotlin Source Code

**Files:**
- Sync: `apps/backend/src/main/kotlin/org/cyoda/uuid/`

- [ ] **Step 1: Replace kotlin source with bloc-portal version**

```bash
rm -rf apps/backend/src/main/kotlin/org/
cp -R ~/dev/bloc-portal/apps/backend/src/main/kotlin/org/ \
      apps/backend/src/main/kotlin/org/
```

- [ ] **Step 2: Verify identical**

```bash
diff -rq apps/backend/src/main/kotlin/ ~/dev/bloc-portal/apps/backend/src/main/kotlin/
```

Expected: No output.

- [ ] **Step 3: Stage kotlin changes**

```bash
git add apps/backend/src/main/kotlin/
```

---

## Task 10: Sync Common Tests

**Files:**
- Delete and replace: `apps/backend/src/test/java/com/java_template/common/`
- Delete and replace: `apps/backend/src/test/java/com/example/`
- Sync: `apps/backend/src/test/kotlin/`
- Sync: `apps/backend/src/test/resources/example/`

- [ ] **Step 1: Replace common test directory**

```bash
rm -rf apps/backend/src/test/java/com/java_template/common/
cp -R ~/dev/bloc-portal/apps/backend/src/test/java/com/java_template/common/ \
      apps/backend/src/test/java/com/java_template/common/
```

- [ ] **Step 2: Replace example test directory**

```bash
rm -rf apps/backend/src/test/java/com/example/
cp -R ~/dev/bloc-portal/apps/backend/src/test/java/com/example/ \
      apps/backend/src/test/java/com/example/
```

- [ ] **Step 3: Replace kotlin test directory**

```bash
rm -rf apps/backend/src/test/kotlin/org/
cp -R ~/dev/bloc-portal/apps/backend/src/test/kotlin/org/ \
      apps/backend/src/test/kotlin/org/
```

- [ ] **Step 4: Replace example test resources**

```bash
rm -rf apps/backend/src/test/resources/example/
cp -R ~/dev/bloc-portal/apps/backend/src/test/resources/example/ \
      apps/backend/src/test/resources/example/
```

- [ ] **Step 5: Remove .DS_Store if copied**

```bash
find apps/backend/src/test/resources/example/ -name ".DS_Store" -delete
```

- [ ] **Step 6: Verify test file counts**

```bash
echo "Common tests:"
find apps/backend/src/test/java/com/java_template/common/ -name "*.java" | wc -l
find ~/dev/bloc-portal/apps/backend/src/test/java/com/java_template/common/ -name "*.java" | wc -l
echo "Example tests:"
find apps/backend/src/test/java/com/example/ -name "*.java" | wc -l
find ~/dev/bloc-portal/apps/backend/src/test/java/com/example/ -name "*.java" | wc -l
```

Expected: Counts match for each pair.

- [ ] **Step 7: Stage all test changes**

```bash
git add apps/backend/src/test/java/com/java_template/common/
git add apps/backend/src/test/java/com/example/
git add apps/backend/src/test/kotlin/
git add apps/backend/src/test/resources/example/
```

---

## Task 11: Commit 2 — Sync Framework Code

- [ ] **Step 1: Review staged changes**

```bash
git diff --cached --stat
```

- [ ] **Step 2: Commit**

```bash
git commit -m "refactor: sync common/ framework code 1:1 with bloc-portal

Replace entire common/ package, kotlin UUID utils, and associated tests
with bloc-portal's versions. Adds OBO auth, observability, expanded gRPC
infrastructure, and additional test coverage.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 12: Add New Resource Directories

**Files:**
- Create: `apps/backend/src/main/resources/api/` (6 files from bloc-portal)
- Create: `apps/backend/src/main/resources/META-INF/` (2 files from bloc-portal)
- Create: `apps/backend/src/main/resources/trino/` (1 file from bloc-portal)

- [ ] **Step 1: Copy api/ directory from bloc-portal**

```bash
cp -R ~/dev/bloc-portal/apps/backend/src/main/resources/api/ \
      apps/backend/src/main/resources/api/
```

- [ ] **Step 2: Copy META-INF/ directory from bloc-portal**

```bash
mkdir -p apps/backend/src/main/resources/META-INF/spring
cp -R ~/dev/bloc-portal/apps/backend/src/main/resources/META-INF/ \
      apps/backend/src/main/resources/META-INF/
```

- [ ] **Step 3: Copy trino/ directory from bloc-portal**

```bash
cp -R ~/dev/bloc-portal/apps/backend/src/main/resources/trino/ \
      apps/backend/src/main/resources/trino/
```

- [ ] **Step 4: Verify files exist**

```bash
ls apps/backend/src/main/resources/api/
ls apps/backend/src/main/resources/META-INF/
ls apps/backend/src/main/resources/META-INF/spring/
ls apps/backend/src/main/resources/trino/
```

Expected:
- api/: 6 openapi YAML files
- META-INF/: `spring.factories` + `spring/` directory
- META-INF/spring/: `org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- trino/: `trino-truststore.p12`

- [ ] **Step 5: Stage new resource directories**

```bash
git add apps/backend/src/main/resources/api/
git add apps/backend/src/main/resources/META-INF/
git add apps/backend/src/main/resources/trino/
```

---

## Task 13: Move Proto Files

**Files:**
- Move: `apps/backend/src/main/proto/*.proto` -> `apps/backend/src/main/resources/proto/`
- Delete: `apps/backend/src/main/proto/`

- [ ] **Step 1: Create target directory**

```bash
mkdir -p apps/backend/src/main/resources/proto/
```

- [ ] **Step 2: Copy proto files from bloc-portal (to ensure 1:1 match)**

```bash
cp ~/dev/bloc-portal/apps/backend/src/main/resources/proto/cloudevents.proto \
   apps/backend/src/main/resources/proto/
cp ~/dev/bloc-portal/apps/backend/src/main/resources/proto/cyoda-cloud-api.proto \
   apps/backend/src/main/resources/proto/
```

- [ ] **Step 3: Remove old proto directory**

```bash
git rm -r apps/backend/src/main/proto/
```

- [ ] **Step 4: Stage new proto location**

```bash
git add apps/backend/src/main/resources/proto/
```

---

## Task 14: Sync Schema Directory

**Files:**
- Replace: `apps/backend/src/main/resources/schema/` with bloc-portal's version

- [ ] **Step 1: Delete current schema directory**

```bash
rm -rf apps/backend/src/main/resources/schema/
```

- [ ] **Step 2: Copy bloc-portal schema directory**

```bash
cp -R ~/dev/bloc-portal/apps/backend/src/main/resources/schema/ \
      apps/backend/src/main/resources/schema/
```

- [ ] **Step 3: Verify identical**

```bash
diff -rq apps/backend/src/main/resources/schema/ \
         ~/dev/bloc-portal/apps/backend/src/main/resources/schema/
```

Expected: No output.

- [ ] **Step 4: Stage schema changes**

```bash
git add apps/backend/src/main/resources/schema/
```

---

## Task 15: Add logback.xml

**Files:**
- Create: `apps/backend/src/main/resources/logback.xml`

- [ ] **Step 1: Copy logback.xml from bloc-portal and adapt**

Copy from bloc-portal, replacing `bloc-portal` with `cyoda-tms`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!--
  ABOUTME: Logback configuration with profile-based output format.
  Default (local dev): human-readable console output.
  "json" profile (K8s/ELK): structured JSON to stdout, one line per log event including stack traces.
  Activate via: SPRING_PROFILES_ACTIVE=cloud or -Dspring.profiles.active=cloud
  -->

<configuration>
    <contextName>cyoda-tms</contextName>

    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Human-readable console output (default, local dev) -->
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>

    <!-- JSON console output for Kubernetes/ELK -->
    <appender name="JSON_STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>
                {"application":"cyoda-tms"}
            </customFields>
            <timestampPattern>yyyy-MM-dd'T'HH:mm:ss.SSS'Z'</timestampPattern>
            <timeZone>UTC</timeZone>
        </encoder>
    </appender>

    <!-- Default: plain text console -->
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

    <!-- When "cloud" profile is active: JSON console only -->
    <springProfile name="cloud">
        <root level="INFO">
            <appender-ref ref="JSON_STDOUT"/>
        </root>
    </springProfile>
</configuration>
```

- [ ] **Step 2: Stage logback.xml**

```bash
git add apps/backend/src/main/resources/logback.xml
```

---

## Task 16: Add applicationExample.yml

**Files:**
- Create: `apps/backend/src/main/resources/applicationExample.yml`

- [ ] **Step 1: Copy from bloc-portal and adapt**

Copy `~/dev/bloc-portal/apps/backend/src/main/resources/applicationExample.yml` and replace:
- `bloc-portal-local` -> `cyoda-tms-local` (in `app.auth.issuer` and `app.obo.issuer`)
- `bloc-portal-key-001` -> `cyoda-tms-key-001` (in `app.obo.key-id`)

Remove (riskblocs-specific, not needed in TMS):
- The `ocr:` section at the bottom
- The `pdf:` section at the bottom

Keep everything else (auth, cors, obo, trino, config sections) as-is since they serve as a template.

- [ ] **Step 2: Stage applicationExample.yml**

```bash
git add apps/backend/src/main/resources/applicationExample.yml
```

---

## Task 17: Merge application.yml

**Files:**
- Modify: `apps/backend/src/main/resources/application.yml`

- [ ] **Step 1: Replace application.yml with bloc-portal's version, adapted for TMS**

The target `application.yml` should be bloc-portal's version with these TMS-specific adaptations:

1. Keep TMS `logging.level.root: ${LOG_LEVEL:INFO}` (env-var driven, more flexible)
2. Keep TMS `server.port: ${SERVER_PORT:8080}` (env-var driven)
3. Change `app.auth.issuer` from `bloc-portal-local` to `cyoda-tms-local`
4. Keep TMS CORS `allowed-origins` with `${CORS_ALLOWED_ORIGINS:...}` env-var override
5. Keep TMS CORS `allow-credentials: true` (TMS uses credential-based auth)
6. Change `app.obo.key-id` from `bloc-portal-key-001` to `cyoda-tms-key-001`
7. Change `app.obo.issuer` from `bloc-portal-local` to `cyoda-tms-local`
8. Keep TMS `app.config.cyoda-host: ${CYODA_HOST}` (no empty default — TMS requires it)
9. Keep TMS gRPC tuning values (larger message sizes for file uploads)
10. Keep TMS `ssl-trust-all: ${SSL_TRUST_ALL:false}` (env-var driven)
11. Keep TMS `include-default-operations: ${INCLUDE_DEFAULT_OPERATIONS:false}` (env-var driven)
12. Keep TMS `entity-base-package`, `skip-ssl`, `execution-mode`, `cyoda-light` config sections
13. Remove `ocr:` section (riskblocs-specific)
14. Remove `pdf:` section (riskblocs-specific)
15. Keep TMS local dev profile section at the bottom

- [ ] **Step 2: Stage application.yml**

```bash
git add apps/backend/src/main/resources/application.yml
```

---

## Task 18: Sync Dockerfile

**Files:**
- Modify: `Dockerfile` (at project root — currently at `apps/backend/Dockerfile` after the move)

- [ ] **Step 1: Check current Dockerfile location**

```bash
ls apps/backend/Dockerfile Dockerfile 2>/dev/null
```

If it's at `apps/backend/Dockerfile`, move it to root:
```bash
git mv apps/backend/Dockerfile ./Dockerfile
```

- [ ] **Step 2: Replace Dockerfile with bloc-portal's version, adapted**

Copy `~/dev/bloc-portal/Dockerfile` and adapt:
- Replace `bloc-portal` references with `cyoda-tms`
- Replace `apps/backend/build/libs/backend-1.0-SNAPSHOT.jar` path — this is correct since both use `apps/backend/` now
- Keep the multi-stage build pattern
- Adapt OTEL_SERVICE_NAME to `cyoda-tms-backend`

If bloc-portal's Dockerfile includes a frontend build stage, keep it but ensure paths reference `apps/frontend/`.

- [ ] **Step 3: Stage Dockerfile**

```bash
git add Dockerfile
```

---

## Task 19: Sync Helm Charts

**Files:**
- Replace: `helm/` directory contents with bloc-portal's version

- [ ] **Step 1: Delete current helm contents**

```bash
rm -rf helm/*
```

- [ ] **Step 2: Copy bloc-portal helm directory**

```bash
cp -R ~/dev/bloc-portal/helm/* helm/
cp ~/dev/bloc-portal/helm/.helmignore helm/ 2>/dev/null || true
```

- [ ] **Step 3: Adapt Chart.yaml**

Edit `helm/Chart.yaml`:
- Change chart `name` from `cyoda-client` to `cyoda-tms`
- Change `description` to reference "Cyoda Test Management System"
- Keep version as-is

- [ ] **Step 4: Adapt values.yaml**

Edit `helm/values.yaml`:
- Replace `bloc-portal` references with `cyoda-tms`
- Adjust image repository names if they reference bloc-portal

- [ ] **Step 5: Stage helm changes**

```bash
git add helm/
```

---

## Task 20: Align build.gradle

**Files:**
- Modify: `apps/backend/build.gradle`

This is the most complex step. Start from bloc-portal's `build.gradle` and adapt.

- [ ] **Step 1: Replace build.gradle with bloc-portal's version**

```bash
cp ~/dev/bloc-portal/apps/backend/build.gradle apps/backend/build.gradle
```

- [ ] **Step 2: Adapt main class references**

Replace all `com.riskblocs.` references with `com.java_template.`:

- `bootJar.mainClass`: `com.riskblocs.Application` -> `com.java_template.Application`
- `bootRun.mainClass`: `com.riskblocs.Application` -> `com.java_template.Application`
- `runApp` default mainClass: `com.riskblocs.Application` -> `com.java_template.Application`
- `bootJarWorkflowImport.mainClass`: `com.riskblocs.common.tool.WorkflowImportTool` -> `com.java_template.common.tool.WorkflowImportTool`
- `validateWorkflowImplementations.mainClass`: `com.riskblocs.common.tool.WorkflowImplementationValidator` -> `com.java_template.common.tool.WorkflowImplementationValidator`
- `validateFunctionalRequirements.mainClass`: `com.riskblocs.common.tool.FunctionalRequirementsValidator` -> `com.java_template.common.tool.FunctionalRequirementsValidator`
- `validateWorkflows.mainClass`: `com.riskblocs.common.tool.WorkflowValidationSuite` -> `com.java_template.common.tool.WorkflowValidationSuite`
- `runTestApp` default mainClass: remove or comment out (TMS may not have `ExampleDataImporter`)

- [ ] **Step 3: Remove riskblocs-specific dependencies**

Remove from the `dependencies` block:
- `implementation 'org.apache.pdfbox:pdfbox:3.0.3'` (PDF extraction — riskblocs only)

- [ ] **Step 4: Add TMS-specific dependencies not in bloc-portal**

Add to `dependencies` block:
- `testImplementation 'org.awaitility:awaitility:4.2.2'` (used by TMS E2E tests)

- [ ] **Step 5: Remove repositories block if present**

If the adapted build.gradle still has a `repositories { mavenCentral() }` block, remove it — this is now in root `settings.gradle`'s `dependencyResolutionManagement`.

- [ ] **Step 6: Verify build.gradle diff is correct**

```bash
diff apps/backend/build.gradle ~/dev/bloc-portal/apps/backend/build.gradle
```

Expected differences should only be:
- `com.riskblocs` -> `com.java_template` replacements
- Removed PDFBox dependency
- Added awaitility test dependency
- Removed/adapted runTestApp default mainClass

- [ ] **Step 7: Stage build.gradle**

```bash
git add apps/backend/build.gradle
```

---

## Task 21: Commit 3 — Sync Resources & Build Config

- [ ] **Step 1: Review all staged changes**

```bash
git diff --cached --stat
```

- [ ] **Step 2: Commit**

```bash
git commit -m "refactor: sync resources and build config with bloc-portal

Add OpenAPI specs, META-INF autoconfig, trino truststore, logback.xml.
Move proto files to resources/proto/. Sync JSON schemas.
Align build.gradle with bloc-portal (OpenAPI codegen, OTel, new deps).
Merge application.yml with bloc-portal structure.
Sync Dockerfile and Helm charts.

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 22: Verify Compilation

- [ ] **Step 1: Run code generation tasks**

```bash
./gradlew :apps:backend:generateProto :apps:backend:generateJsonSchema2Pojo :apps:backend:generateOpenApi
```

Expected: BUILD SUCCESSFUL. If any fail, diagnose and fix the issue (likely a path problem in build.gradle).

- [ ] **Step 2: Compile Java**

```bash
./gradlew :apps:backend:compileJava
```

Expected: BUILD SUCCESSFUL. If there are compilation errors, they will likely be:
- TMS `application/` classes importing `common/` classes that changed signature
- Missing imports for new classes from the synced `common/`

Fix any compilation errors before proceeding.

- [ ] **Step 3: Compile Kotlin**

```bash
./gradlew :apps:backend:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Compile tests**

```bash
./gradlew :apps:backend:compileTestJava :apps:backend:compileTestKotlin
```

Expected: BUILD SUCCESSFUL. If test compilation fails, fix the issues.

---

## Task 23: Verify Tests Pass

- [ ] **Step 1: Run unit tests**

```bash
./gradlew :apps:backend:test
```

Expected: BUILD SUCCESSFUL with all tests passing. If tests fail:
- Check if test failures are due to changed `common/` class signatures
- Check if test failures are due to new Spring autoconfig from `META-INF/`
- Fix each failure individually

- [ ] **Step 2: Document any fixes needed**

If any fixes were needed in steps above, note them for the commit message.

---

## Task 24: Commit 4 — Verification & Fixes

- [ ] **Step 1: Check if there are any changes to commit**

```bash
git status
git diff
```

If no changes needed (everything compiled and tested clean), skip to Task 25.

- [ ] **Step 2: Stage and commit fixes**

```bash
git add -A
git commit -m "fix: resolve compilation and test issues after framework sync

[describe specific fixes made]

Co-Authored-By: Claude Opus 4.6 (1M context) <noreply@anthropic.com>"
```

---

## Task 25: Final Verification

- [ ] **Step 1: Run full build**

```bash
./gradlew :apps:backend:build
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Verify common/ is still 1:1 with bloc-portal**

```bash
diff -rq apps/backend/src/main/java/com/java_template/common/ \
         ~/dev/bloc-portal/apps/backend/src/main/java/com/java_template/common/
```

Expected: No output.

- [ ] **Step 3: Verify monorepo structure**

```bash
ls settings.gradle build.gradle nx.json pnpm-workspace.yaml gradlew
ls apps/backend/build.gradle apps/backend/project.json
ls apps/frontend/package.json
```

Expected: All files exist.

- [ ] **Step 4: Verify no tracked files that shouldn't be**

```bash
git ls-files | grep -E '(\.gradle/|\.DS_Store|bun\.lock)'
```

Expected: No output.

---

## Task 26: Create Pull Request

- [ ] **Step 1: Push branch**

```bash
git push -u origin refactor/monorepo-restructure
```

- [ ] **Step 2: Create PR**

```bash
gh pr create \
  --title "Restructure as monorepo and sync framework with bloc-portal" \
  --body "$(cat <<'EOF'
## Summary

- Restructured project as monorepo: `backend/` and `frontend/` moved under `apps/`
- Added Nx + pnpm monorepo orchestration with root-level Gradle wrapper
- Synced `common/` framework code 1:1 with bloc-portal (adds OBO auth, observability, expanded gRPC)
- Synced resources: OpenAPI specs, META-INF autoconfig, proto files, JSON schemas, logback, trino
- Aligned `build.gradle` with bloc-portal (OpenAPI codegen, OTel, updated deps)
- Synced Dockerfile and Helm charts
- Cleaned up tracked files that shouldn't be in git (.gradle cache, .DS_Store, bun lockfiles)

## Test plan

- [ ] `./gradlew :apps:backend:build` passes (compile + unit tests)
- [ ] `./gradlew :apps:backend:compileJava :apps:backend:compileKotlin` clean compilation
- [ ] `./gradlew :apps:backend:test` all unit tests pass
- [ ] `diff -rq` confirms common/ is 1:1 with bloc-portal
- [ ] `pnpm install` at root succeeds
- [ ] Monorepo structure matches bloc-portal layout
- [ ] No .gradle cache, .DS_Store, or bun lockfiles tracked in git

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: Return PR URL**
