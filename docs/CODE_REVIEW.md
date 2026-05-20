# Code Review — Cyoda TMS

**Reviewed:** 2026-05-20  
**Scope:** Full codebase — backend (`apps/backend/`) + frontend (`apps/frontend/`)  
**Stack:** Java 21 + Kotlin / Spring Boot 3.5 / Gradle · React 18 + TypeScript + Vite  

---

## How to Use This Document

Issues are grouped by **priority tier**. Work through them top-to-bottom.  
Each issue includes: file path with line numbers, root cause, and concrete fix.

| Tier | Label | Meaning |
|------|-------|---------|
| 🔴 | **Critical** | Must fix before any production deployment |
| 🟠 | **Important** | Fix in the current or next sprint |
| 🟡 | **Suggestion** | Low-risk cleanup / backlog |

---

## What Was Checked

All findings below were manually verified by reading the source files.  
No finding was included without locating the exact code.

---

## 🔴 Critical — Fix Before Production

---

### CR-01 · JWT is Base64 encoding with no signature — roles can be forged

**File:** `apps/backend/src/main/java/com/java_template/application/auth/JwtTokenProvider.java`

**Lines 21–26:**
```java
String payload = username + "|" + role + "|" + issuedAt + "|" + expiresAt;
String token = Base64.getEncoder().encodeToString(payload.getBytes());
```

**Lines 75–96 (fallback path):**
```java
// Fallback: decode from token (Base64 format: username|role|issuedAt|expiresAt)
String payload = new String(Base64.getDecoder().decode(token));
String[] parts = payload.split("\\|");
if (parts.length >= 2) {
    long expiresAt = Long.parseLong(parts[3]);
    if (expiresAt > System.currentTimeMillis()) {
        return parts[1]; // Return role
    }
}
```

**Problem:**  
The token is plain Base64, not a cryptographically signed JWT. The in-memory `tokenStore` provides protection only during the lifetime of the current server process. After a restart, `tokenStore` is empty and the code falls back to decoding the raw Base64 payload. Because there is no signature, any client can base64-encode `username|ADMIN|<past>|<future>` and submit it as a valid ADMIN token after a server restart. The `SECRET` constant on line 14 is dead code — it is declared but never used anywhere.

**Fix:**  
Replace with [JJWT](https://github.com/jwtk/jjwt) or Nimbus JOSE. Sign with HS256 using `app.auth.secret` from `application.yml` (the property is already declared in config — it just isn't wired). Remove the fallback Base64-decode path entirely.

---

### CR-02 · Auth cookie missing `Secure` flag

**File:** `apps/backend/src/main/java/com/java_template/application/controller/AuthController.java`

**Lines 51–57:**
```java
Cookie cookie = new Cookie(COOKIE_NAME, authResponse.token);
cookie.setHttpOnly(true);
cookie.setPath("/");
cookie.setMaxAge(COOKIE_MAX_AGE);
cookie.setAttribute("SameSite", "Lax");
httpResponse.addCookie(cookie);
```

**Problem:**  
`cookie.setSecure(true)` is never called. The browser can transmit this cookie over plain HTTP connections (misconfigured load balancer, HTTP health-check path, redirect). `HttpOnly` alone does not prevent transmission over HTTP.

**Fix:**  
Add `cookie.setSecure(true)`. Gate it on a configurable property so local HTTP dev still works:
```java
if (secureCookies) cookie.setSecure(true);
```

---

### CR-03 · Raw exception messages returned in 500 responses

**Files and lines (all confirmed):**

| File | Line | Code |
|------|------|------|
| `AttachmentController.java` | 54 | `e.getMessage() != null ? e.getMessage() : "Upload failed"` returned in body |
| `AttachmentController.java` | 247 | `e.getMessage() != null ? e.getMessage() : "Copy failed"` returned in body |
| `GlobalSearchController.java` | 123 | `"Search failed: " + e.getMessage()` returned in body |

`SearchController.java` lines 102 and 130 re-throw as `RuntimeException("... " + e.getMessage(), e)` — Spring's default `/error` mapping will include this in the response under some configurations.

**Problem:**  
Internal exception messages can contain file paths, class names, Cyoda API error details, or stack snippets. This violates CLAUDE.md Gate 3 which requires `CyodaExceptionUtil` and forbids exposing internal details.

**Fix:**  
Log the full exception, return a generic user-facing message:
```java
} catch (Exception e) {
    logger.error("Upload failed for project {}: {}", projectId, e.getMessage(), e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "File upload failed. Please try again."));
}
```

---

## 🟠 Important — Fix This Sprint

---

### IM-01 · `DefectController` has no role check on mutating endpoints

**File:** `apps/backend/src/main/java/com/java_template/application/controller/DefectController.java`

The controller has no `HttpServletRequest` injection and no role guard anywhere. Any authenticated user — including TESTER — can `POST`, `PUT`, or `DELETE` defects. This contradicts the RBAC spec applied in `ProjectController`, `SuiteController`, and `TestCaseController`.

Compare with `ProjectController` which checks:
```java
String role = (String) request.getAttribute("role");
if (!"ADMIN".equals(role)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
```

**Fix:**  
Add role check to `createDefect`, `updateDefect`, `deleteDefect`. Determine which roles should be allowed (ADMIN only, or TESTER+ADMIN) and apply consistently.

---

### IM-02 · `AtomicFailureProcessor` is a no-op stub wired SYNC on every step transition

**File:** `apps/backend/src/main/java/com/java_template/application/processor/AtomicFailureProcessor.java`

**Workflow:** `apps/backend/src/main/resources/workflow/testrunstep/version_1/TestRunStep.json`

The processor body:
```java
EntityProcessorCalculationResponse response = new EntityProcessorCalculationResponse();
response.setId(request.getId());
response.setSuccess(true);
return response;
```

The workflow JSON registers it as `"executionMode":"SYNC"` on every transition (`set_passed`, `set_failed`, `set_skipped`, `reset_untested`) in every state (`untested`, `passed`, `failed`, `skipped`). This means every single step status change makes a synchronous gRPC round-trip to a processor that does nothing.

**Related stubs (not wired to any workflow JSON — dead beans):**
- `BugLinkProcessor.java`
- `MetricsAggregatorProcessor.java`
- `EdgeMessageProcessor.java`
- `TestRunCompleteProcessor.java`

**Fix options:**
1. Implement `AtomicFailureProcessor` with real logic (detect failures, update counters).
2. Or remove it from the workflow JSON if it serves no current purpose (eliminate the dead gRPC round-trip).
3. Delete the four unwired stub beans entirely.

Also: `processorName` is declared as a non-static instance field in all five stubs:
```java
private final String processorName = "AtomicFailureProcessor"; // should be private static final
```

---

### IM-03 · `BugLinkService` uses in-memory repository — data is not persisted in Cyoda

**Files:**
- `apps/backend/src/main/java/com/java_template/application/service/BugLinkService.java`
- `apps/backend/src/main/java/com/java_template/application/repository/BugLinkRepository.java`

`BugLinkService` injects `BugLinkRepository` (a `ConcurrentHashMap` in-memory store) instead of `EntityService`. All bug link data is lost on server restart. `BugLinkService` is not injected into any controller, making the entire service + repository effectively dead API code.

**Fix:**  
Migrate `BugLinkService` to use `EntityService` (same pattern as `DefectService`), or remove the service and repository if bug links are handled elsewhere.

---

### IM-04 · Seven in-memory repositories are dead Spring beans

**Directory:** `apps/backend/src/main/java/com/java_template/application/repository/`

The following `@Repository` classes are annotated Spring beans but are not injected by any service (confirmed by grep):

- `TestRunRepository.java`
- `SuiteRepository.java`
- `ProjectRepository.java`
- `TestCaseRepository.java`
- `TestRunCaseRepository.java`
- `TestRunStepRepository.java`
- `AttachmentRepository.java`

(Note: `BugLinkRepository` is injected by `BugLinkService` — see IM-03.)

These classes are loaded at startup, allocate `ConcurrentHashMap` instances, and serve no purpose. They are confusing — they imply there is a secondary data store, but there isn't.

**Fix:** Delete all seven files.

---

### IM-05 · `TestRunController.deleteTestRun` can never return 404

**File:** `apps/backend/src/main/java/com/java_template/application/controller/TestRunController.java`

**Lines 130–135:**
```java
if (testRunService.deleteTestRun(id)) {
    return ResponseEntity.noContent().build();
}
return ResponseEntity.notFound().build(); // ← dead code
```

**Service:** `TestRunService.java` lines 289–291:
```java
public boolean deleteTestRun(UUID id) {
    entityService.deleteById(id);
    return true; // always
}
```

The service unconditionally returns `true`. The 404 branch in the controller is unreachable. Deleting a non-existent run always returns 204.

**Fix:**  
Either check for existence before deleting (one extra `getById`), or restructure `deleteTestRun` to throw a domain exception when the entity is not found, and catch it in the controller.

---

### IM-06 · `SearchController` has double `/api` prefix — all endpoints are unreachable

**File:** `apps/backend/src/main/java/com/java_template/application/controller/SearchController.java`

**Line 37:**
```java
@RequestMapping("/api/v1/projects/{projectId}/search")
```

**`application.yml` line 9:**
```yaml
context-path: /api
```

The Spring servlet context path is `/api`. The controller mapping adds another `/api`, making the effective URL `/api/api/v1/projects/{projectId}/search`. No client calls this path.

Compare with `GlobalSearchController.java` line 22 which is correctly mapped:
```java
@RequestMapping("/v1/search") // effective: /api/v1/search ✓
```

**Fix:**  
Change `SearchController`'s mapping to `/v1/projects/{projectId}/search`.  
Also note: `SearchController` and `GlobalSearchController` duplicate the global search logic. Consider consolidating.

---

### IM-07 · TOCTOU in `TestRunController.updateTestRun`

**File:** `apps/backend/src/main/java/com/java_template/application/controller/TestRunController.java`

**Lines 92–96:**
```java
if (!testRunService.testRunExists(id)) {         // getById round-trip #1
    return ResponseEntity.notFound().build();
}
testRun.setProjectId(projectId);
TestRunDTO updated = testRunService.updateTestRun(id, testRun); // getById round-trip #2
```

`testRunExists` calls `getTestRunById` → `entityService.getById`. Then `updateTestRun` calls `getTestRunById` again. Two round-trips for the same entity. If the run is deleted between these calls, the caller gets a 500 instead of 404. This also adds unnecessary latency.

`updateTestRun` already returns the merged DTO — the pre-check is redundant.

**Fix:**  
Remove the `testRunExists` guard. Handle the not-found case inside `updateTestRun` (return `Optional<TestRunDTO>`) and map `Optional.empty()` to 404 in the controller.

---

### IM-08 · Static `ObjectMapper` in `TestRunDTO` bypasses Spring's configured mapper

**File:** `apps/backend/src/main/java/com/java_template/application/dto/TestRunDTO.java`

**Line 91:**
```java
private static final ObjectMapper objectMapper = new ObjectMapper();
```

This is a second, unconfigured `ObjectMapper` instance that bypasses any module registrations done on the Spring-managed bean (e.g. `JavaTimeModule`, custom serializers). DTOs should not own infrastructure objects.

**Fix:**  
Move `getStepStatusesAsMap()` and `setStepStatusesFromMap()` to a dedicated utility class or into `TestRunService` where an injected `ObjectMapper` is already available.

---

### IM-09 · `getTestRunsByProjectId` loads ALL runs globally for client-side filtering

**File:** `apps/backend/src/main/java/com/java_template/application/service/TestRunService.java`

**Lines 130–138:**
```java
List<TestRunDTO> all = entityService.findAll(MODEL_SPEC, TestRunDTO.class)
        .data().stream()
        .map(this::withId)
        .filter(r -> projectId.equals(r.getProjectId()))
        .toList();
```

The comment explains this is a workaround because `projectId` was not in the schema at registration time. As the total run count across all projects grows, this loads the entire collection into memory on every request (before cache warmup).

`getAllTestRunsByProjectId` (line 158) has the same pattern.

**Fix:**  
Re-register the `TestRun` schema with `projectId` indexed, then replace `findAll + filter` with `entityService.search(MODEL_SPEC, conditionByField("projectId", ...), ...)`.

---

### IM-10 · Eight `log.warn("===== ... =====")` debug markers in production code

**Files and confirmed line numbers:**

| File | Lines |
|------|-------|
| `TestRunService.java` | 84, 107, 191, 205, 211, 233, 238, 254 |
| `TestRunController.java` | 118 |

Example:
```java
log.warn("===== CREATETESTRUN ENTRY ===== projectId={}, ...", ...);
log.warn("===== UNLOCK_RUN FORBIDDEN ===== role={} is not TESTER or ADMIN", role);
```

WARN level is for actionable, unexpected production conditions. These are trace-level debugging artifacts that flood production logs. The `UNLOCK_RUN FORBIDDEN` warn at `TestRunController:118` is the most reasonable (it is an access-control event) but should still be `log.info`.

**Fix:**  
Demote all `=====`-style markers to `log.debug(...)` or remove them. The race-condition fix they were added for is resolved.

---

### IM-11 · `RunExecution.tsx` is 1826 lines — too large to maintain safely

**File:** `apps/frontend/src/pages/RunExecution.tsx` (1826 lines, confirmed)

This single file handles: execution state machine, attachment uploads, defect creation, step status toggling, virtual list rendering, and multiple modals. Past bug fixes (defect badge, auto-advance, TestRunCase resolution) all required scattered changes across this file.

**Fix:**  
Decompose into focused components and hooks before adding the next feature:
- `useRunExecution(runId)` — state, API calls, auto-save
- `StepList` / `StepRow` — virtual list rendering
- `DefectPanel` — defect creation and display
- `EvidencePanel` — attachment upload

---

### IM-12 · Token stored in `localStorage` regardless of environment

**File:** `apps/frontend/src/lib/api.ts`

**Lines 16–23:**
```typescript
export function setAuthToken(token: string | null) {
  storedToken = token;
  if (token) {
    localStorage.setItem('auth_token', token);  // ← always, not DEV-gated
  } else {
    localStorage.removeItem('auth_token');
  }
}
```

Line 43 gates the `Authorization` header on `import.meta.env.DEV`, but the `localStorage.setItem` on line 19 runs unconditionally. Any XSS on the same origin can read `localStorage` and extract the token, bypassing the `HttpOnly` cookie's XSS protection.

**Fix:**  
Gate `localStorage.setItem` on `import.meta.env.DEV`:
```typescript
if (token && import.meta.env.DEV) {
  localStorage.setItem('auth_token', token);
}
```
In production, rely solely on the `HttpOnly` cookie.

---

### IM-13 · `ProjectService.getProjectById` blocks the request thread with `Thread.sleep`

**File:** `apps/backend/src/main/java/com/java_template/application/service/ProjectService.java`

**Lines 157–173:**
```java
for (int attempt = 1; attempt <= 3; attempt++) {
    try {
        ...
        return Optional.of(withId(result));
    } catch (Exception e) {
        if (attempt < 3) {
            Thread.sleep(500); // blocks Tomcat thread
        }
    }
}
```

Up to 1 000 ms of blocked Tomcat thread per slow request. Under concurrency this exhausts the thread pool.

**Fix:**  
Return the created DTO directly from `createProject` without a re-fetch (the entity data is already available after `entityService.create`), eliminating the need for this retry loop in the post-create path. For genuine read-your-writes needs, use `CompletableFuture` with a scheduled executor, not `Thread.sleep`.

---

## 🟡 Suggestions — Backlog

---

### SG-01 · `ProjectCounterService` has no unit tests — highest-risk untested code

**File:** `apps/backend/src/main/java/com/java_template/application/service/ProjectCounterService.java`

No `ProjectCounterServiceTest.java` exists (confirmed). This service contains the most complex concurrent logic in the codebase: synchronized blocks, `ConcurrentHashMap` monitor objects, bootstrap scanning for max `displayId`, and batch-reservation logic. It is mocked in other tests but never tested in isolation.

**Fix:** Add unit tests covering: bootstrap scan correctness, concurrent reservation (multiple threads calling `nextRunDisplayId` simultaneously), and the case where Cyoda returns no existing counters.

---

### SG-02 · No E2E feature files for Suite or TestCase CRUD

**Directory:** `apps/backend/src/test/resources/features/`

Existing feature files: `health-check.feature`, `project-crud.feature`, `defect-crud.feature`, `test-run-crud.feature`.  
Missing: Suite CRUD, TestCase CRUD, Attachment upload.

Suites and TestCases are core entities. Their Cyoda integration is only validated through mocked unit tests.

---

### SG-03 · `console.log` calls fire on every page load in production

**Files and confirmed lines:**

| File | Lines | Issue |
|------|-------|-------|
| `AuthContext.tsx` | 27, 31, 34 | 3× `console.log` on every mount |
| `Attachments.tsx` | 116 | `console.log('📸 View URL generated:', ...)` |

`RunExecution.tsx` lines 419, 500, 716, 746 use `console.warn`/`console.error` which are legitimate error-path logging and are acceptable.

**Fix:** Remove the `AuthContext.tsx` debug logs and the `Attachments.tsx` log. If dev-time logging is needed, gate on `import.meta.env.DEV`.

---

### SG-04 · `processorName` is a non-static instance field in stub processors

**Files:** All five stub processor files.

```java
private final String processorName = "AtomicFailureProcessor"; // instance field
```

Should be:
```java
private static final String PROCESSOR_NAME = "AtomicFailureProcessor";
```

One extra heap allocation per Spring bean. Minor but inconsistent with `SnapshotProcessor` which uses `PROCESSOR_NAME`.

---

### SG-05 · Stale test display names reference removed "hardcoded users"

**File:** `apps/backend/src/test/java/com/java_template/application/controller/AuthControllerTest.java`

Test method names at lines 134 and 151 reference "Only 2 hardcoded users exist." This was true before commit `606e2bd7` which migrated to `AuthUsersProperties` (env-var-based). The names are now misleading.

---

### SG-06 · Add `ssl-trust-all: true` startup guard for non-dev profiles

**File:** `apps/backend/src/main/resources/application.yml` line 157:
```yaml
ssl-trust-all: ${SSL_TRUST_ALL:false}
```

CLAUDE.md calls this "test-only". Default is `false`, but there is no code guard that rejects startup if `ssl-trust-all: true` is set in a production profile.

**Fix:** Add a `@PostConstruct` check (similar to `AuthUsersProperties.validate()`) that throws if `ssl-trust-all: true` and the active Spring profile is not `local` or `test`.

---

### SG-07 · No MIME-type validation on attachment upload

**Files:** `AttachmentController.java` (backend), `api.ts` (frontend)

No file type or extension check on either side. Only `spring.servlet.multipart.max-file-size` limits content. An `AcceptedMimeTypes` allowlist on the controller and a soft file-type check on the frontend would prevent accidental uploads of executables or scripts.

---

### SG-08 · `GlobalSearchController` and `SearchController` duplicate global search logic

Both controllers implement near-identical logic: get all projects, search each, aggregate, sort by score, paginate. `SearchController` is unreachable (see IM-06). After fixing IM-06, consolidate the two into one with clear per-project vs. global endpoint separation.

---

## What Is Done Well

The following areas are solid and should be preserved:

- **Constructor injection** is used everywhere. No `@Autowired` field injection found in any application class.
- **`common/` framework code is untouched.** Only `application/` contains business logic.
- **Criteria are pure functions.** `RequireAdminRoleCriterion`, `RequireTesterOrAdminRoleCriterion`, `SystemActionOnlyCriterion` have no side effects. ✓
- **`SnapshotProcessor`** correctly avoids calling `EntityService` on the entity being processed — creates separate `TestRunCase` entities instead. ✓
- **`ProjectCounterService`** uses per-project `ConcurrentHashMap` monitor objects — no global lock contention. ✓
- **`TestRunService.updateTestRun`** makes a single `getById` call for both race-condition detection and merge logic, with clear explanatory comment. ✓
- **Bean Validation** (`@NotBlank`, `@Size`, `@Valid`) is applied correctly at controller boundaries. ✓
- **`AuthUsersProperties.validate()`** with `@PostConstruct` is a correct fail-fast pattern. ✓
- **E2E test tags** (`@smoke`, `@requires-cyoda`) allow CI to run stateless tests without a live Cyoda instance. ✓
- **`/runs/{id}/details` batch endpoint** is a sound architecture decision — eliminates the two-request waterfall that caused the 10s+ latency in Run Execution view. ✓
- **`@CacheEvict`/`@Cacheable` strategy** is coherent: all mutating operations evict, TTL is short. ✓

---

## Completed Fixes

| # | Issue | Status | What was done |
|---|-------|--------|---------------|
| CR-01 | JWT is Base64 with no signature | ✅ Done | Added JJWT 0.12.6 to `build.gradle`. Rewrote `JwtTokenProvider`: removed `tokenStore`, `TokenData`, hardcoded `SECRET`, and Base64 fallback path. Now uses `Keys.hmacShaKeyFor` + `Jwts.builder().signWith(key)` with HS256. Secret injected via `@Value("${app.auth.secret}")`. Public API unchanged — `AuthorizationFilter` and `AuthService` needed no edits. 9 tests added in `JwtTokenProviderTest`, including `rejectsForgedBase64Token` which proves the attack vector is closed. `.env.example` updated with `APP_AUTH_SECRET` and `openssl rand -base64 32` generation hint. |
| CR-02 | Auth cookie missing `Secure` flag | ✅ Done | Added `@Value("${app.auth.secure-cookie:true}") boolean secureCookie` to `AuthController` constructor. Added `cookie.setSecure(secureCookie)` on both login and logout cookies. New property `app.auth.secure-cookie` in `application.yml` (default `true`). `.env.example` sets `APP_AUTH_SECURE_COOKIE=false` for local HTTP dev. Test `loginCookieHasSecureFlag` added to `AuthControllerTest`. |
| CR-03 | Raw `e.getMessage()` in 500 responses | ✅ Done | `AttachmentController:54,247` — replaced `e.getMessage()` with generic strings `"File upload failed…"` / `"File copy failed…"`, exception logged at ERROR. `GlobalSearchController:123` — replaced `"Search failed: " + e.getMessage()` with `"Search failed. Please try again."`. `SearchController:102,130` — removed `e.getMessage()` from re-thrown `RuntimeException` message (low risk, but consistent). New test files `AttachmentControllerErrorTest` and `GlobalSearchControllerErrorTest` verify sensitive details never reach the response body. |
| IM-01 | `DefectController` has no role check | ✅ Done | Added `HttpServletRequest` to `createDefect`, `updateDefect`, `deleteDefect`. POST/PUT require TESTER or ADMIN (TESTERs file defects during test runs). DELETE requires ADMIN only (audit trail). 8 tests in `DefectRoleAccessControlTest` cover all combinations. |
| IM-06 | `SearchController` double `/api` prefix | ✅ Done | Changed `@RequestMapping("/api/v1/projects/{projectId}/search")` → `/v1/projects/{projectId}/search` (one line). With server context-path `/api` effective URL is now correctly `/api/v1/projects/{id}/search`. 2 tests in `SearchControllerMappingTest` verify POST and quick-search GET are reachable. |
| SG-01 | `ProjectCounterService` has no unit tests | ✅ Done | 17 tests in `ProjectCounterServiceTest` covering: correct TC/TR/DEF/REP formats, counter increment, batch reservation (one update per batch), bootstrap when no counter exists, bootstrap above existing max ID, zero-field migration path, in-memory cache (search called once for N calls), `initializeCounterForProject` (all fields = 1, failure swallowed), `deleteCounterForProject` (deleteById + cache eviction, no-op when absent), and concurrent access (5 threads → 5 unique IDs). |
| SG-03 | `console.log` on every page load | ✅ Done | Removed 3 `console.log` calls from `AuthContext.tsx` (fired on every mount). Removed `console.log('📸 View URL generated:...')` from `Attachments.tsx`. `noConsoleLogs.test.ts` scans source files and fails if pattern returns. |
| SG-04 | `processorName` instance field in processors | ✅ Done | Replaced `private final String processorName` with `private static final String PROCESSOR_NAME` in all 5 stub processors (AtomicFailure, BugLink, EdgeMessage, MetricsAggregator, TestRunComplete). Consistent with `SnapshotProcessor`. |
| SG-05 | Stale "hardcoded users" test display names | ✅ Done | Updated `@DisplayName` on two tests in `AuthControllerTest`: "Only 2 hardcoded users exist - admin/tester" → "Configured admin/tester user can login". |
| SG-07 | No MIME-type validation on attachment upload | ✅ Done | `AttachmentController`: added `ALLOWED_MIME_TYPES` Set (images, PDF, plain text, CSV, ZIP, Office). Null or disallowed content-type → 400 with descriptive message, logged at WARN. Frontend `attachmentsApi.upload`: added client-side check before fetch — returns `Promise.reject` with friendly error for disallowed types. 11 tests: 6 backend parameterized (`AttachmentMimeTypeTest`) + 5 frontend (`api.upload.test.ts`). |
| SG-08 | Duplicate global search in `SearchController` | ✅ Done | Removed `@GetMapping globalSearch()` method from `SearchController` — it implemented global cross-project search that ignored the `{projectId}` path variable, duplicating `GlobalSearchController` (`/v1/search`). `SearchController` now has only per-project endpoints: `POST /v1/projects/{id}/search` and `GET /v1/projects/{id}/search/quick`. `SearchControllerDuplicationTest` verifies `GET` on the class path returns 405. Removed unused imports (`ProjectDTO`, `SearchResultDTO`, `Collectors`, etc.). |
| SG-06 | No startup guard for `ssl-trust-all=true` | ✅ Done | Added `SslTrustAllGuard` (`@Component`) with `@PostConstruct` that throws `IllegalStateException` if `ssl-trust-all=true` and no safe profile (`local`, `test`, `cucumber`, `dev`) is active. 5 tests in `SslTrustAllGuardTest`. |
| IM-13 | `Thread.sleep` retry in `ProjectService` | ✅ Done | Removed 3-attempt retry loop with `Thread.sleep(500)` from `ProjectService.getProjectById`. Replaced with a single try/catch — on failure returns `Optional.empty()` immediately (404 to caller, no thread blocking). `createProject` was already returning the entity directly from `entityService.create`, so this retry served only the general GET path. Test `getProjectById_notFound_makesExactlyOneAttempt` verifies `entityService.getById` is called exactly once on failure. |
| IM-12 | Token in `localStorage` without DEV guard | ✅ Done | `setAuthToken`: `localStorage.setItem` now gated on `import.meta.env.DEV`. `getAuthToken`: `localStorage.getItem` fallback also gated on DEV. `localStorage.removeItem` (logout cleanup) always runs regardless of mode. 5 tests in `api.auth.test.ts` cover DEV/prod behaviour for both functions using `import.meta.env` mutation. |
| IM-10 | `log.warn("===== ... =====")` debug markers | ✅ Done | Removed all 8 `===== ` markers from `TestRunService` — the race condition they diagnosed is fixed (IM-07). The concurrent `initialize_run` retry log demoted to `log.info`. `TestRunController` unlock-rejected warn kept at WARN (real security event) but `=====` style removed. `NoDebugWarnMarkersTest` uses source-file scan to verify the pattern is gone. |
| IM-08 | Static `ObjectMapper` in `TestRunDTO` | ✅ Done | Removed `static final ObjectMapper objectMapper`, `static final Logger log`, and methods `getStepStatusesAsMap()`/`setStepStatusesFromMap()` from `TestRunDTO`. Added private `parseStepStatuses(String)` and `serializeStepStatuses(Map)` helpers to `TestRunService` using its already-injected `ObjectMapper`. Updated merge logic in `updateTestRun` to use the helpers. `DemoSeederService` updated to call `objectMapper.writeValueAsString()` directly. Structural test `TestRunDTOStructureTest` verifies the DTO has no `ObjectMapper` field. |
| IM-07 | TOCTOU double `getById` in `updateTestRun` | ✅ Done | `TestRunService.updateTestRun` now returns `Optional<TestRunDTO>` — returns `Optional.empty()` early if `getTestRunById` finds nothing, eliminating the second round-trip. `TestRunController.updateTestRun` drops the `testRunExists` pre-check, delegates entirely to the Optional result. Controller tests updated: `testRunExists` mocks replaced by `Optional`-returning `updateTestRun` mocks. New service test `updateTestRun_notFound_returnsEmpty` verifies `entityService.update` is never called for missing runs. |
| IM-05 | `deleteTestRun` always returns 204 | ✅ Done | `TestRunService.deleteTestRun()` now calls `getTestRunById(id)` first — returns `false` (→ 404) if run not found, only calls `entityService.deleteById` when found. `testDeleteTestRun` updated with `getById` stub. New test `deleteTestRun_notFound_returnsFalse` verifies 404 path and confirms `deleteById` is never called for non-existent runs. |
| IM-04 | 7 dead in-memory repository beans | ✅ Done | Deleted `TestRunRepository`, `SuiteRepository`, `ProjectRepository`, `TestCaseRepository`, `TestRunCaseRepository`, `TestRunStepRepository`, `AttachmentRepository` — confirmed zero references in main and test code before deletion. `BugLinkRepository` kept (used by `BugLinkService`, tracked separately as IM-03). 7 tests in `DeadRepositoryBeansTest` verify none of the deleted classes are registered as Spring beans. | `AttachmentController:54,247` — replaced `e.getMessage()` with generic strings `"File upload failed…"` / `"File copy failed…"`, exception logged at ERROR. `GlobalSearchController:123` — replaced `"Search failed: " + e.getMessage()` with `"Search failed. Please try again."`. `SearchController:102,130` — removed `e.getMessage()` from re-thrown `RuntimeException` message (low risk, but consistent). New test files `AttachmentControllerErrorTest` and `GlobalSearchControllerErrorTest` verify sensitive details never reach the response body. |

---

## Recommended Execution Order

Work on one issue at a time. Suggested order:

1. **CR-01** — Fix JWT (security foundation; everything else depends on auth being trustworthy)
2. **CR-02** — Add `Secure` cookie flag (5 min change, high impact)
3. **CR-03** — Replace `e.getMessage()` in 500 responses (20 min, 3 files)
4. **IM-06** — Fix `SearchController` double `/api` mapping (1-line fix, unblocks search feature)
5. **IM-01** — Add role check to `DefectController`
6. **IM-04** — Delete 7 dead repository classes
7. **IM-03** — Migrate `BugLinkService` to `EntityService` or delete it
8. **IM-02** — Decide on stub processors (implement or remove from workflow JSON)
9. **IM-05** — Fix `deleteTestRun` always-true return
10. **IM-07** — Remove TOCTOU pre-check in `updateTestRun`
11. **IM-08** — Move `ObjectMapper` out of `TestRunDTO`
12. **IM-10** — Demote debug `log.warn` markers to `log.debug`
13. **IM-12** — Gate `localStorage` token storage on DEV
14. **IM-09** — Fix `getTestRunsByProjectId` global fetch (requires schema re-registration)
15. **IM-11** — Decompose `RunExecution.tsx` (larger refactor, do before next feature)
16. **IM-13** — Remove `Thread.sleep` retry in `ProjectService`
17. **SG-01–08** — Backlog, address as capacity allows
