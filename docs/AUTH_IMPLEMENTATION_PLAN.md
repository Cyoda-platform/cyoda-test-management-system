# Authentication in Cyoda TMS: Audit & Implementation Plan

## 1. Current State

### 1.1 Custom Authentication System (`application/auth/`)

| Component | What it does | Problem |
|-----------|-------------|---------|
| `AuthUsersProperties` | Loads users from env vars (`APP_USERS_0_*`) | Passwords stored in **plaintext** — compared via `.equals()` |
| `JwtTokenProvider` | Issues HMAC-SHA256 JWTs signed with `APP_AUTH_SECRET` | `sub` = username string, not Cyoda UUID; no `aud`, no `jti`; no explicit algorithm allow-list |
| `AuthService` | Verifies password, calls `JwtTokenProvider.generateToken()` | Plaintext password comparison |
| `AuthorizationFilter` | Extracts token from cookie / Bearer header, validates it, stores username + role in **request attributes** | NOT integrated with Spring Security `SecurityContextHolder` — `OboAwareAuthentication` cannot see the user identity |
| `TMSAuthConfig` | Registers `AuthorizationFilter` as a Servlet Filter | — |
| `User` | Simple POJO: username + password + role | No Cyoda UUID |
| `RequireRole` | Annotation on controller methods | Only applied to some write operations; READ is not checked |

**Summary:** Authentication works for the browser, but is completely isolated from Spring Security. All Cyoda calls are made under the M2M service account — never under the acting user.

---

### 1.2 OBO Infrastructure in `common/auth/` — EXISTS but is NOT USED

The full OBO mechanism is already implemented in framework code:

| Component | What it does |
|-----------|-------------|
| `Authentication` | M2M client_credentials flow with token cache |
| `OboAwareAuthentication` | **Dispatch point**: if `SecurityContextHolder` holds a `JwtAuthenticationToken` → OBO exchange; otherwise → M2M |
| `OboTokenService` | RFC 8693 token exchange with a Caffeine cache keyed by user UUID |
| `SubjectTokenSigner` | Signs RS256 subject tokens with `sub=userUUID`, `user_roles`, `caas_org_id` |
| `OboKeyRegistrationService` | RSA key lifecycle: generate → AES-GCM encrypt → store in Cyoda entity → register public key with Cyoda; daily rotation |
| `AesGcmEncryption` | AES-256-GCM with 96-bit CSPRNG IV |
| `EventAuthContextHandler` | Reads `authtype`/`authid`/`authclaims` from CloudEvent, installs `SecurityContext`, restores it afterward |
| `DefaultEventUserResolver` | Fallback — trusts `authid` without verification (with `logger.warn`) |
| `OboProperties` | OBO configuration (`app.obo.*`) |

**Why OBO never fires:** `OboAwareAuthentication.getAccessToken()` checks whether `SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken`. But `AuthorizationFilter` writes to `request.setAttribute(...)`, not to the `SecurityContext` — so the OBO branch is never reached.

---

### 1.3 Spring Security Configuration (`common/config/SecurityConfig.java`)

The fallback `SecurityFilterChain` uses `anyRequest().permitAll()` — it is only activated when the application provides no `SecurityFilterChain` bean of its own. Currently the application provides none, so this permissive chain is active. All real protection relies entirely on the custom `AuthorizationFilter`.

---

### 1.4 Authorization — Criteria and RBAC

| Component | Situation |
|-----------|----------|
| `@RequireRole("ADMIN")` | Present on some endpoints in `ProjectController`, `SuiteController`, etc. Reads role from `request.getAttribute("role")` |
| `RequireAdminRoleCriterion` | FSM criterion that **always returns `true`** — role is not enforced at the Cyoda transition level |
| READ operations | No role check at all |

---

### 1.5 Current Configuration (`application.yml`)

```yaml
app:
  auth:
    secret: ${APP_AUTH_SECRET:change-me-dev-secret-32-chars-min}
    secure-cookie: ${APP_AUTH_SECURE_COOKIE:true}
  obo:
    encryption-key: ${APP_OBO_ENCRYPTION_KEY:}
    admin-client-id: ${CYODA_CLIENT_ID:}    # same M2M account — no separation of duties
    key-id: cyoda-tms-key-001
    issuer: cyoda-tms-local
    validity-days: 90
  event:
    auth-context:
      mode: REQUIRED
```

OBO is fully configured in YAML. The only missing piece is proper Spring Security integration so that `SecurityContextHolder` is populated when a user JWT is present.

---

## 2. Required Security Guarantees (Level L1)

Per the Cyoda Auth Pattern Guide, **L1** applies to internal tools with no regulatory requirements — a reasonable baseline for TMS in its current stage.

### 2.1 Mandatory Controls (required at every level)

| Code | Requirement | Current Status |
|------|-------------|----------------|
| **F-1** | Credential verification before token issuance (Argon2id/bcrypt) | ❌ Plaintext |
| **F-3** | Deny-by-default authorization (deny when no authorizer is registered) | ⚠️ Partial — custom filter blocks requests but is not integrated with Spring Security |
| **F-4** | READ goes through the same `authorize()` hook as mutations | ❌ READ is unchecked |
| **F-5** | Application `EventUserResolver` verifies User entity exists | ❌ `DefaultEventUserResolver` trusts `authid` without verification |
| **F-9** | TLS on every endpoint carrying tokens | ⚠️ `ssl-trust-all: true` dev-only, guard exists |
| **F-11** | 96-bit CSPRNG IV per AES-GCM encryption, never reused | ✅ Implemented in `AesGcmEncryption` (common/) |
| **F-19** | KEK length validated at boot | ✅ Implemented in `AesGcmEncryption` (common/) |
| **F-21** | `alg=none` rejected; explicit algorithm allow-list in JWT verifier | ❌ No explicit allow-list in `JwtTokenProvider.validateToken()` |

### 2.2 Additional L1 Requirements (from the guide)

| Requirement | Status |
|------------|--------|
| `sub` = Cyoda UUID of the user | ❌ Currently `sub` = username string |
| OBO token exchange for user-attributed Cyoda calls | ❌ OBO never fires |
| User entity in Cyoda | ❌ Does not exist |
| `SecurityContextHolder` populated on JWT validation | ❌ |
| `EventUserResolver` verifies User entity | ❌ |

---

## 3. Target Architecture

```
Browser / API client
        │
        │ POST /auth/login (username + password)
        ▼
┌─────────────────────────────────────────────────────┐
│ AuthController                                       │
│  1. AuthService.authenticate()                       │
│     - look up UserEntity in Cyoda by username        │
│     - BCrypt.verify(password, storedHash)            │
│     - issueJwt(sub=UUID, roles=[...], aud, jti)      │
│  2. Set httpOnly cookie + return token in body       │
└─────────────────────────────────────────────────────┘
        │
        │ Subsequent requests: Bearer <JWT> or cookie
        ▼
┌─────────────────────────────────────────────────────┐
│ ApiSecurityConfig  (new SecurityFilterChain)         │
│  - OAuth2 resource server JWT decoder                │
│  - algorithm allow-list: HS256 only                  │
│  - validates aud, exp, iss                           │
│  - installs JwtAuthenticationToken → SecurityContext │
│  - public paths: /auth/login, /auth/logout,          │
│                  /actuator/**, /swagger/**            │
└─────────────────────────────────────────────────────┘
        │
        │ JwtAuthenticationToken in SecurityContextHolder
        ▼
┌─────────────────────────────────────────────────────┐
│ OboAwareAuthentication.getAccessToken()              │
│  SecurityContext has JwtAuthenticationToken?          │
│    YES → OboTokenService.getOboToken(UUID, roles)    │
│            → sign subject token (RS256, sub=UUID)    │
│            → POST /oauth/token (RFC 8693 exchange)   │
│            → OBO access token  (cached by UUID)      │
│    NO  → M2M client_credentials token                │
└─────────────────────────────────────────────────────┘
        │
        │ OBO / M2M token on Cyoda gRPC / HTTP calls
        ▼
     Cyoda: audit trail records the real user

        ──── Cyoda callback (workflow event) ────
        ▼
┌─────────────────────────────────────────────────────┐
│ EventAuthContextHandler.establish(cloudEvent)        │
│  - reads authtype / authid / authclaims              │
│  - authtype="user" → TmsEventUserResolver.resolve()  │
│      - fetches UserEntity from Cyoda by authid=UUID  │
│      - verifies state = ACTIVE                       │
│      - on failure → fatal: abort event               │
│  - installs synthetic JwtAuthenticationToken         │
│  - after handler: restore previous SecurityContext   │
└─────────────────────────────────────────────────────┘
```

---

## 4. Implementation Plan

### Phase A — M2M (already complete ✅)
`Authentication` works. `OboKeyRegistrationService` bootstraps the RSA signing key at startup.

---

### Phase B — User Entity + Login + Spring Security Integration

#### B1. User Entity Schema and Workflow

New file: `workflow/user/version_1/User.json`

**FSM states:**
```
PENDING_ACTIVATION
   └─ auto ──► ACTIVE    (validation passes: non-blank username, valid role)
   └─ auto ──► ERRORED   (validation fails)
ACTIVE
   └─ manual ► INACTIVE
   └─ manual ► LOCKED
INACTIVE / LOCKED
   └─ manual ► ACTIVE
```

**User entity fields:**
```json
{
  "username":     "string, unique natural key",
  "email":        "string, informational",
  "passwordHash": "bcrypt hash — never the raw password",
  "roles":        ["ADMIN" | "TESTER"],
  "createdAt":    "ISO instant",
  "lastLoginAt":  "ISO instant (nullable)"
}
```

The Cyoda UUID of this entity becomes the `sub` claim in every JWT issued for this user.

#### B2. Changes to `AuthService`

Remove `AuthUsersProperties` (plaintext users in config go away).

New `authenticate(username, password)` flow:
1. Search Cyoda for a UserEntity with the given `username`.
2. Verify entity state = `ACTIVE` (deny if `LOCKED` or `INACTIVE`).
3. `BCryptPasswordEncoder.matches(password, entity.passwordHash)`.
4. Issue JWT with:
   - `sub` = `entity.metadata().getId().toString()` (the Cyoda UUID)
   - `roles` = entity roles list
   - `aud` = `"cyoda-tms"`
   - `jti` = `UUID.randomUUID().toString()`
   - `iss` = `app.auth.issuer`
   - `exp` = now + `app.auth.token-ttl-seconds`

#### B3. Replace `AuthorizationFilter` with `ApiSecurityConfig`

New bean `SecurityFilterChain` in `application/config/ApiSecurityConfig.java`:

```java
http
  .csrf(AbstractHttpConfigurer::disable)
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(authz -> authz
      .requestMatchers("/auth/login", "/auth/logout",
                       "/actuator/**", "/swagger-ui/**",
                       "/v3/api-docs/**", "/webjars/**").permitAll()
      .anyRequest().authenticated()
  )
  .oauth2ResourceServer(oauth2 -> oauth2
      .jwt(jwt -> jwt
          .decoder(
              NimbusJwtDecoder
                  .withSecretKey(hmacKey)
                  .macAlgorithm(MacAlgorithm.HS256)  // rejects alg=none and RS*/ES*
                  .build()
          )
          .jwtAuthenticationConverter(jwtAuthenticationConverter())
      )
  );
```

This replaces `AuthorizationFilter`. Spring Security validates the JWT, pins the algorithm to HS256, and populates `SecurityContextHolder` with a `JwtAuthenticationToken`. `OboAwareAuthentication` begins working immediately — no other changes needed.

The `jwtAuthenticationConverter` must map the `roles` claim to `GrantedAuthority` objects so that `@PreAuthorize("hasRole('ADMIN')")` works as expected.

#### B4. User Bootstrap — Bulk Import from Resource File

`APP_USERS_*` env vars are removed. Users are provisioned from a YAML seed file at startup.

**Seed file:** `apps/backend/src/main/resources/users-seed.yml`

```yaml
users:
  - username: admin
    email: admin@example.com
    passwordHash: "$2b$12$..."   # bcrypt hash — never a plaintext password
    roles: [ADMIN]
  - username: tester1
    email: tester1@example.com
    passwordHash: "$2b$12$..."
    roles: [TESTER]
  - username: tester2
    email: tester2@example.com
    passwordHash: "$2b$12$..."
    roles: [TESTER]
```

**Rules:**
- The file is committed to the repository. It **must never contain plaintext passwords** — only pre-computed bcrypt hashes.
- A companion script (`scripts/hash-password.sh`) wraps `htpasswd -bnBC 12 "" <password>` so operators can generate correct hashes without writing code.
- The file path can be overridden via `APP_USERS_SEED_FILE` env var for environments that mount secrets from a vault.

**`UserSeederRunner`** (implements `CommandLineRunner`, runs at startup):
1. Parse `users-seed.yml` (or the file at `APP_USERS_SEED_FILE`).
2. For each entry, search Cyoda for an existing UserEntity with the same `username`.
   - **Exists** → skip (no overwrite — prevents accidental role/password resets in production).
   - **Absent** → create the UserEntity and wait for it to reach `ACTIVE`.
3. Log a summary: `N users already present, M users created`.
4. If the seed file is missing or empty, log a warning but do **not** abort startup — the application is usable if users were already seeded in a previous run.

**Re-running the import** (e.g. adding a new team member) is safe at any time: existing users are skipped, only new entries are created. To update an existing user's password or role, an ADMIN must use the `POST /admin/users/{id}` endpoint (see open question in §9).

---

### Phase C — Authorization Hardening

#### C1. Algorithm Allow-List (F-21)

`NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256)` accepts **only** HS256, automatically rejecting `alg=none` and algorithm-confusion attacks (e.g. an RSA public key used to verify an HMAC signature). This is wired inside `ApiSecurityConfig` as part of B3.

#### C2. READ Through `authorize()` (F-4)

`.anyRequest().authenticated()` in the new `SecurityFilterChain` enforces authentication on every request including GET. For L1 this is sufficient: any authenticated, active user may read any resource. Ownership checks are deferred to L2.

#### C3. Active-User Gating

Checking `state = ACTIVE` at login time (B2) covers the most common case. For L2, add a per-request check via `@PreAuthorize` or a Spring Security method-security expression that reads the User entity state — this is not required for L1.

---

### Phase D — OBO (infrastructure ready, just needs wiring)

After Phase B3 (Spring Security populates `SecurityContextHolder`), OBO **works automatically** via `OboAwareAuthentication`. The only actions required are operational:

1. Ensure `APP_OBO_ENCRYPTION_KEY` is set in the deployment environment.
2. Confirm `OboKeyRegistrationService.onStartup()` successfully registers the key with Cyoda (check startup logs).
3. Confirm all application services reach `Authentication` via the `@Primary` `OboAwareAuthentication` bean — no direct `Authentication.getAccessToken()` injection should exist in application code.

---

### Phase E — CloudEvent User Resolver (F-5)

Create `TmsEventUserResolver` in `application/auth/`:

```java
@Component
public class TmsEventUserResolver implements EventUserResolver {

    private final EntityService entityService;

    public TmsEventUserResolver(EntityService entityService) {
        this.entityService = entityService;
    }

    @Override
    public EventUserIdentity resolve(CloudEventAuthContext authContext) {
        // SecurityContext is cleared before this call — M2M is used for Cyoda lookups

        // 1. Validate UUID format
        UUID userId;
        try {
            userId = UUID.fromString(authContext.authId());
        } catch (IllegalArgumentException e) {
            throw new EventUserResolutionException(
                "Invalid authid format — expected UUID, got: " + authContext.authId());
        }

        // 2. Verify user exists and is active
        EntityWithMetadata<UserEntity> user =
            entityService.getEntity(userId, UserEntity.class);  // throws if absent
        if (!"ACTIVE".equals(resolveState(user))) {
            throw new EventUserResolutionException(
                "User " + userId + " is not in ACTIVE state");
        }

        // 3. Return identity with roles from authclaims
        List<String> roles = AuthClaimsParser.parseRoles(
            objectMapper, authContext.authClaimsJson());
        return new EventUserIdentity(userId.toString(), roles);
    }
}
```

Once this bean is registered, `DefaultEventUserResolver` is no longer active (it is annotated `@ConditionalOnMissingBean`). The framework stops trusting `authid` without verification.

---

## 5. File Change Summary

### New files to create
| File | Purpose |
|------|---------|
| `application/entity/UserEntity.java` | Cyoda entity for a user |
| `application/config/ApiSecurityConfig.java` | Spring Security JWT resource server — replaces `AuthorizationFilter` |
| `resources/workflow/user/version_1/User.json` | User entity FSM |
| `application/auth/TmsEventUserResolver.java` | CloudEvent user verification |
| `application/service/UserService.java` | Cyoda lookup by username; user creation |
| `application/config/UserSeederRunner.java` | Reads `users-seed.yml` at startup and creates missing users in Cyoda |
| `resources/users-seed.yml` | Seed file: username + email + bcrypt passwordHash + roles |
| `scripts/hash-password.sh` | Helper: wraps `htpasswd` to generate bcrypt hashes for the seed file |

### Files to modify
| File | Change |
|------|--------|
| `AuthService.java` | Remove `AuthUsersProperties` dependency; add bcrypt + Cyoda User lookup |
| `JwtTokenProvider.java` | Add `aud`, `jti` claims; delegate algorithm pinning to `ApiSecurityConfig` |
| `TMSAuthConfig.java` | Remove `AuthorizationFilter` registration (`ApiSecurityConfig` replaces it) |
| `application.yml` | Remove `app.users` block; add `app.seed-file` config (path override for `users-seed.yml`) |
| `.env.example` | Remove `APP_USERS_*`; add `APP_USERS_SEED_FILE` (optional) |

### Files to delete
| File | Reason |
|------|--------|
| `AuthorizationFilter.java` | Replaced by Spring Security resource server |
| `AuthUsersProperties.java` | Users move from config into Cyoda entity store |
| `User.java` (application/auth/) | Replaced by `UserEntity` |

---

## 6. Execution Order and Dependencies

```
B1 (User workflow JSON + UserEntity)
  └─► B2 (UserService + AuthService refactor — bcrypt, Cyoda lookup)
        └─► B3 (ApiSecurityConfig — Spring Security resource server)
              │                        ↑
              │               OBO starts working here automatically
              │
              └─► B4 (UserSeederRunner — bulk import from users-seed.yml)
                    └─► C1 (alg allow-list — already inside ApiSecurityConfig/B3)
                          └─► C2 (READ enforcement — already inside ApiSecurityConfig/B3)
                                └─► E (TmsEventUserResolver)
```

Phase D (OBO) has no dedicated implementation tasks — it becomes operational the moment B3 is in place and `APP_OBO_ENCRYPTION_KEY` is set.

---

## 7. What Is Deferred (not L1)

| Control | Level | Why deferred |
|---------|-------|--------------|
| `jti` revocation list in middleware | L3 | Requires in-memory / Redis store; 3600 s token TTL is acceptable at L1 |
| Two separate M2M accounts (routine + admin) | L2 | Requires Cyoda Cloud configuration; same account is the current constraint |
| Audit log of all auth decisions | L2 | Logging infrastructure not ready |
| Refresh tokens + short access TTL (≤ 900 s) | L2 | Additional frontend complexity; not justified at L1 |
| KMS-backed subject token signing | L3 | Over-engineering for current deployment stage |
| `aud` + `jti` on subject tokens | L2 | Already in `SubjectTokenSigner`; just needs enabling via `OboProperties` |
| Active-user gating on every request (per-call entity check) | L2 | Login-time check is sufficient for L1 |
| Field-level authorization | L3 | Not applicable to current data model |

---

## 8. Environment Variables After Implementation

```bash
# Removed
APP_USERS_0_USERNAME=admin      # ← REMOVE
APP_USERS_0_PASSWORD=...        # ← REMOVE (plaintext password — gone)
APP_USERS_1_USERNAME=tester     # ← REMOVE
APP_USERS_1_PASSWORD=...        # ← REMOVE

# Kept
APP_AUTH_SECRET=...             # HMAC-SHA256 key for JWT signing (min 32 bytes)
APP_OBO_ENCRYPTION_KEY=...      # base64-encoded AES-256 KEK (was already required)

# New
APP_USERS_SEED_FILE=            # optional override path for users-seed.yml
                                # defaults to classpath resources/users-seed.yml
```

Passwords are no longer in env vars. They live as bcrypt hashes inside `users-seed.yml` (committed) or in a vault-mounted file pointed to by `APP_USERS_SEED_FILE`. Use `scripts/hash-password.sh <password>` to generate a hash before editing the seed file.

---

## 9. Open Questions

1. **User creation endpoint:** Should there be a `POST /admin/users` REST endpoint (ADMIN-only) for creating users via the API, or is bootstrap-via-seeder sufficient for the current use case?
2. **Password reset flow:** Is self-service password reset in scope, or is it an admin operation only?
3. **Multi-tenant:** All users currently share one Cyoda tenant. Does this need to change?
