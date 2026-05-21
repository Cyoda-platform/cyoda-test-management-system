@e2e @tms @auth
Feature: User Authentication
  As a TMS user
  I want to authenticate with my username and password
  So that I can access protected API endpoints

  # -------------------------------------------------------------------------
  # These scenarios require a live Cyoda instance with the User entity
  # schema and workflow imported. User entities are created by UserSeederRunner
  # on application startup from users-seed.yml (test resources).
  #
  # Auth endpoints (context-path /api):
  #   POST /api/auth/login   — public, returns { token, username, role, expiresAt }
  #                                           + httpOnly cookie "auth-token"
  #   POST /api/auth/logout  — public, clears the cookie
  #
  # Protected endpoint used as a probe:
  #   GET  /api/projects     — any authenticated user
  #
  # Tags:
  #   @smoke         — no entity operations, tests auth layer only
  #   @requires-cyoda — needs live Cyoda + seeded User entities
  # -------------------------------------------------------------------------

  # ── Login success ─────────────────────────────────────────────────────────

  @smoke @requires-cyoda
  Scenario: Admin can log in with correct credentials
    When I log in as "admin" with password "admin123"
    Then the login response status is 200
    And the login response contains a "token" field
    And the login response contains field "username" with value "admin"
    And the login response contains field "role" with value "ADMIN"
    And an httpOnly cookie "auth-token" is set

  @smoke @requires-cyoda
  Scenario: Tester can log in with correct credentials
    When I log in as "tester" with password "tester123"
    Then the login response status is 200
    And the login response contains field "role" with value "TESTER"

  @smoke @requires-cyoda
  Scenario: JWT subject is a UUID, not the username
    When I log in as "admin" with password "admin123"
    Then the login response status is 200
    And the token subject claim is a valid UUID

  # ── Login rejection ───────────────────────────────────────────────────────

  @smoke
  Scenario: Login with wrong password is rejected
    When I log in as "admin" with password "wrong-password"
    Then the login response status is 401

  @smoke
  Scenario: Login with unknown username is rejected
    When I log in as "nobody" with password "doesNotMatter"
    Then the login response status is 401

  @smoke
  Scenario: Login endpoint is accessible without a prior token
    When I call the login endpoint with empty credentials
    Then the login response status is 400

  # ── Token-based access ────────────────────────────────────────────────────

  @requires-cyoda
  Scenario: Bearer token from login grants access to protected endpoints
    Given I am logged in as admin
    When I GET "/api/projects"
    Then the response status is 200

  @requires-cyoda
  Scenario: Cookie from login grants access to protected endpoints
    Given I have logged in and stored the cookie for "admin" with password "admin123"
    When I GET "/api/projects" using the auth cookie
    Then the response status is 200

  @smoke
  Scenario: Request without a token is rejected with 401
    When I GET "/api/projects" without a token
    Then the response status is 401

  @smoke
  Scenario: Request with a tampered token is rejected with 401
    When I GET "/api/projects" with bearer token "not.a.valid.jwt"
    Then the response status is 401

  # ── Logout ───────────────────────────────────────────────────────────────

  @smoke
  Scenario: Logout clears the auth cookie
    When I call the logout endpoint
    Then the logout response status is 204
    And the "auth-token" cookie is expired
