Feature: Test Run CRUD Operations and Lifecycle

  Background:
    Given I am logged in as admin
    And I have valid bearer token
    And a project with ID exists

  Scenario: Create a new test run
    Given I prepare a TestRunDTO with name "Test Run 001"
    When I POST to "/projects/{projectId}/runs"
    Then the response HTTP status is 201
    And the response body contains "name": "Test Run 001"

  Scenario: Get all test runs by project
    Given test runs exist for the project
    When I GET from "/projects/{projectId}/runs"
    Then the response HTTP status is 200
    And the response body has "data" array with at least 1 element

  Scenario: Get test run by ID
    Given a test run with ID exists
    When I GET from "/projects/{projectId}/runs/{runId}"
    Then the response HTTP status is 200
    And the response body contains "status": "active"

  Scenario: Get test run with details (includes cases)
    Given a test run with ID and associated test cases exist
    When I GET from "/projects/{projectId}/runs/{runId}/details"
    Then the response HTTP status is 200
    And the response body has "run" object
    And the response body has "runCases" array

  Scenario: Update test run
    Given a test run with ID exists
    And I prepare a TestRunDTO with name "Updated Test Run"
    When I PUT to "/projects/{projectId}/runs/{runId}"
    Then the response HTTP status is 200
    And the response body contains "name": "Updated Test Run"

  Scenario: Complete test run
    Given a test run with ID exists
    When I POST to "/projects/{projectId}/runs/{runId}/complete"
    Then the response HTTP status is 200
    And the response body contains "status": "completed"

  Scenario: Unlock test run with Admin role
    Given a test run with ID exists
    And the test run is completed
    When I POST to "/projects/{projectId}/runs/{runId}/unlock" with role "Admin"
    Then the response HTTP status is 200

  Scenario: Unlock test run with Tester role
    Given a test run with ID exists
    And the test run is completed
    When I POST to "/projects/{projectId}/runs/{runId}/unlock" with role "Tester"
    Then the response HTTP status is 200

  Scenario: Unlock test run without authentication returns 401
    Given a test run with ID exists
    And the test run is completed
    When I POST to "/projects/{projectId}/runs/{runId}/unlock" without role
    Then the response HTTP status is 401

  Scenario: Delete test run
    Given a test run with ID exists
    When I DELETE from "/projects/{projectId}/runs/{runId}"
    Then the response HTTP status is 204

  Scenario: Get non-existent test run returns 404
    Given a non-existent test run ID
    When I GET from "/projects/{projectId}/runs/{runId}"
    Then the response HTTP status is 404

  Scenario: Create test run case
    Given a test run with ID exists
    When I POST a test case to "/projects/{projectId}/runs/{runId}/cases"
    Then the response HTTP status is 201
    And the response body has "id" field

  Scenario: Update test run case status
    Given a test run case exists
    When I PUT to "/projects/{projectId}/runs/{runId}/cases/{caseId}/status" with status "PASSED"
    Then the response HTTP status is 200
    And the response body contains "status": "PASSED"

  Scenario: Link bug to test run case
    Given a test run case exists
    When I POST to "/projects/{projectId}/runs/{runId}/cases/{caseId}/link-bug" with bugUrl "https://jira.example.com/BUG-123"
    Then the response HTTP status is 200
    And the response body contains "bugUrl": "https://jira.example.com/BUG-123"

  Scenario: Create test run with blank name returns 400
    Given I prepare a TestRunDTO with name ""
    When I POST to "/projects/{projectId}/runs"
    Then the response HTTP status is 400
