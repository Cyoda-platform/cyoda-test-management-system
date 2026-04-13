@e2e @tms
Feature: Project REST API — CRUD lifecycle
  As a Test Manager I need to create, read, update and delete projects
  so that I can organise test suites and cases inside them.

  # -----------------------------------------------------------------------
  # Auth model (AuthController / AuthService / AuthorizationFilter):
  #   POST /api/auth/login  → 200 + JSON body { token, username, role, expiresAt }
  #                                           + httpOnly cookie "auth-token"
  #   The filter accepts EITHER the cookie OR "Authorization: Bearer <token>"
  #   Hardcoded users: admin / admin123 (role ADMIN), tester / tester123 (role TESTER)
  #
  # Project API (ProjectController):
  #   POST   /api/projects          — ADMIN only → 201 Created
  #   GET    /api/projects          — any auth   → 200 { data:[], pageNumber, ... }
  #   GET    /api/projects/{id}     — any auth   → 200 | 404
  #   PUT    /api/projects/{id}     — ADMIN only → 200 | 404
  #   DELETE /api/projects/{id}     — ADMIN only → 204 | 404
  #
  # Validation rules on ProjectDTO:
  #   name        @NotBlank, @Size(max=255)
  #   description @Size(max=1000)
  #
  # Scenarios marked @requires-cyoda call EntityService (needs a live Cyoda
  # instance).  Scenarios marked @smoke only exercise auth / validation layers
  # and do NOT call EntityService.
  # -----------------------------------------------------------------------

  # ── Authentication ──────────────────────────────────────────────────────

  @smoke
  Scenario: Admin can log in and receives a token in the response body
    When I log in as "admin" with password "admin123"
    Then the auth response HTTP status is 200
    And the auth response body contains field "token"
    And the auth response body contains field "username" with value "admin"
    And the auth response body contains field "role" with value "ADMIN"
    And the "auth-token" httpOnly cookie is set

  @smoke
  Scenario: Tester can log in and receives role TESTER
    When I log in as "tester" with password "tester123"
    Then the auth response HTTP status is 200
    And the auth response body contains field "role" with value "TESTER"

  @smoke
  Scenario: Invalid credentials are rejected with 401
    When I log in as "admin" with password "wrong-password"
    Then the auth response HTTP status is 401

  # ── Unauthenticated access ───────────────────────────────────────────────

  @smoke
  Scenario: Creating a project without an auth token returns 401
    Given I am not authenticated
    When I POST to "/api/projects" with body:
      """
      {"name":"Anon Project","description":"Should be rejected"}
      """
    Then the response HTTP status is 401

  # ── Input validation (no Cyoda needed — fails at @Valid boundary) ────────

  @smoke
  Scenario: Creating a project with a blank name returns 400
    Given I am logged in as admin
    When I POST to "/api/projects" with body:
      """
      {"name":"","description":"Missing name"}
      """
    Then the response HTTP status is 400

  @smoke
  Scenario: Creating a project with a name longer than 255 chars returns 400
    Given I am logged in as admin
    When I POST to "/api/projects" with body:
      """
      {"name":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","description":"Too long"}
      """
    Then the response HTTP status is 400

  # ── Role-based access control (no Cyoda needed — fails at role check) ───

  @smoke
  Scenario: TESTER role cannot create a project — expects 403 Forbidden
    Given I am logged in as tester
    When I POST to "/api/projects" with body:
      """
      {"name":"TESTER attempt","description":"Must be blocked"}
      """
    Then the response HTTP status is 403

  @smoke
  Scenario: TESTER role cannot delete a project — expects 403 Forbidden
    Given I am logged in as tester
    When I DELETE "/api/projects/00000000-0000-0000-0000-000000000001"
    Then the response HTTP status is 403

  # ── Full CRUD (requires live Cyoda instance) ────────────────────────────

  @requires-cyoda
  Scenario: Admin creates a project — response must be 201 CREATED with all required fields
    Given I am logged in as admin
    When I create a project named "E2E Lifecycle Project" with description "Created by Cucumber"
    Then the create project response HTTP status is 201
    And the project response body contains field "id"
    And the project response body contains field "name" with value "E2E Lifecycle Project"
    And the project response body contains field "createdAt"
    And the project response body contains field "updatedAt"

  @requires-cyoda
  Scenario: Admin can retrieve the project just created by its ID
    Given I am logged in as admin
    And I have created a project named "GetById Project" with description "Fetch me"
    When I GET the last created project by ID
    Then the get project response HTTP status is 200
    And the project response body contains field "name" with value "GetById Project"
    And the project response body contains field "id"

  @requires-cyoda
  Scenario: GET all projects returns a paged response
    Given I am logged in as admin
    When I GET "/api/projects"
    Then the response HTTP status is 200
    And the paged response body contains the "data" array field
    And the paged response body contains the "totalElements" field

  @requires-cyoda
  Scenario: Admin can update a project name
    Given I am logged in as admin
    And I have created a project named "Original Name" with description "Will be updated"
    When I update the last created project name to "Updated Name"
    Then the update project response HTTP status is 200
    And the project response body contains field "name" with value "Updated Name"

  @requires-cyoda
  Scenario: Admin can delete a project and it returns 204 No Content
    Given I am logged in as admin
    And I have created a project named "To Be Deleted" with description "Cleanup test"
    When I delete the last created project
    Then the delete response HTTP status is 204

  # ── BUG-002 documentation scenario ─────────────────────────────────────
  # EXPECTED: deleting a non-existent project should return 404 Not Found.
  # ACTUAL:   ProjectService.deleteProject() always returns true regardless
  #           of whether the entity exists. The controller's 404 branch is
  #           unreachable dead code. The Cyoda platform may throw a runtime
  #           exception (→ 500) or silently accept the delete (→ 204).
  # STATUS:   This scenario documents BUG-002. It is expected to FAIL until
  #           the bug is fixed. Do NOT change this test to match the bug.
  # FIX:      See BUG-002 in the audit report below.
  # ────────────────────────────────────────────────────────────────────────
  @requires-cyoda @bug @bug-002
  Scenario: Deleting a non-existent project should return 404 Not Found [BUG-002]
    Given I am logged in as admin
    When I DELETE "/api/projects/00000000-0000-0000-0000-000000000099"
    Then the response HTTP status is 404
