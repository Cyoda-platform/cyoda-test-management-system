@e2e @tms
Feature: Defects REST API — CRUD lifecycle
  As a Test Manager I need to create, read, update and delete defects
  so that I can track bugs found during test runs.

  # Auth model is shared with project-crud.feature (see ProjectApiSteps).
  #
  # Defects API (DefectController):
  #   POST   /api/projects/{projectId}/defects              → 201 Created
  #   GET    /api/projects/{projectId}/defects              → 200 { data:[], totalElements, ... }
  #   GET    /api/projects/{projectId}/defects?testRunId=.. → 200 filtered by run
  #   GET    /api/projects/{projectId}/defects/{id}         → 200 | 404
  #   PUT    /api/projects/{projectId}/defects/{id}         → 200 | 404
  #   DELETE /api/projects/{projectId}/defects/{id}         → 204
  #
  # Validation rules on DefectDTO:
  #   title    @NotBlank, @Size(max=255)
  #   severity @NotBlank
  #
  # @smoke    — does NOT call EntityService; no live Cyoda instance required.
  # @requires-cyoda — calls EntityService; needs a live Cyoda instance.

  # ── Authentication ────────────────────────────────────────────────────────

  @smoke
  Scenario: Unauthenticated POST to defects returns 401
    Given I am not authenticated
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/defects" with body:
      """
      {"title":"Bug","severity":"Major"}
      """
    Then the response HTTP status is 401

  # ── Input validation (no Cyoda needed — fails at @Valid boundary) ─────────

  @smoke
  Scenario: Creating a defect with a blank title returns 400
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/defects" with body:
      """
      {"title":"","severity":"Major"}
      """
    Then the response HTTP status is 400

  @smoke
  Scenario: Creating a defect with a blank severity returns 400
    Given I am logged in as admin
    When I POST to "/api/projects/00000000-0000-0000-0000-000000000001/defects" with body:
      """
      {"title":"Login button is broken","severity":""}
      """
    Then the response HTTP status is 400

  # ── Full CRUD (requires live Cyoda instance) ──────────────────────────────

  @requires-cyoda
  Scenario: Admin creates a defect — response must be 201 with all required fields
    Given I am logged in as admin
    And I have created a defect testing project named "Defect CRUD Project"
    When I create a defect with title "Login button is broken" and severity "Major"
    Then the create defect response HTTP status is 201
    And the defect response body contains field "id"
    And the defect response body contains field "title" with value "Login button is broken"
    And the defect response body contains field "severity" with value "Major"
    And the defect response body contains field "displayId"
    And the defect response body contains field "createdAt"

  @requires-cyoda
  Scenario: Admin can retrieve the defect just created by its ID
    Given I am logged in as admin
    And I have created a defect testing project named "Defect GetById Project"
    And I have created a defect with title "Fetch Defect" and severity "Minor"
    When I GET the last created defect by ID
    Then the get defect response HTTP status is 200
    And the defect response body contains field "title" with value "Fetch Defect"

  @requires-cyoda
  Scenario: GET all defects for a project returns a paged response
    Given I am logged in as admin
    And I have created a defect testing project named "List Defects Project"
    And I have created a defect with title "Listed Defect" and severity "Critical"
    When I GET defects for the last created project
    Then the response HTTP status is 200
    And the paged response body contains the "data" array field
    And the paged response body contains the "totalElements" field

  @requires-cyoda
  Scenario: Admin can update a defect severity and status
    Given I am logged in as admin
    And I have created a defect testing project named "Update Defect Project"
    And I have created a defect with title "Original Defect" and severity "Minor"
    When I update the last created defect severity to "Critical" and status to "In Progress"
    Then the update defect response HTTP status is 200
    And the defect response body contains field "severity" with value "Critical"
    And the defect response body contains field "status" with value "In Progress"

  @requires-cyoda
  Scenario: Admin can delete a defect and it returns 204 No Content
    Given I am logged in as admin
    And I have created a defect testing project named "Delete Defect Project"
    And I have created a defect with title "Delete Me" and severity "Minor"
    When I delete the last created defect
    Then the delete defect response HTTP status is 204

  @requires-cyoda
  Scenario: Create a defect linked to a test run — testRunId is persisted
    Given I am logged in as admin
    And I have created a defect testing project named "Run-linked Defect Project"
    When I create a defect with title "Run-linked Bug" and severity "Major" and testRunId "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    Then the create defect response HTTP status is 201
    And the defect response body contains field "testRunId" with value "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"

  @requires-cyoda
  Scenario: GET defects filtered by testRunId returns only matching defects
    Given I am logged in as admin
    And I have created a defect testing project named "Filter Defects Project"
    And I have created a defect with title "Run Defect" and severity "Major" and testRunId "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    And I have created a defect with title "Unrelated Defect" and severity "Minor"
    When I GET defects for the last created project filtered by testRunId "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    Then the response HTTP status is 200
    And the filtered defect list contains 1 item
    And the first filtered defect has title "Run Defect"
