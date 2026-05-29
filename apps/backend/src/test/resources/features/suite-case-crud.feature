Feature: Suite and Test Case CRUD Operations

  Background:
    Given I am logged in as admin
    And I have valid bearer token
    And I have a suite testing project

  Scenario: TESTER cannot create a suite — expects 403 Forbidden
    When I log in as tester and POST a suite to the project
    Then the response HTTP status is 403

  Scenario: Admin creates a suite — returns 201 with required fields
    When I create a suite named "My Suite"
    Then the response HTTP status is 201
    And the suite response body contains "name": "My Suite"

  Scenario: Admin can get all suites for the project
    Given I have created a suite named "Listable Suite"
    When I GET all suites for the project
    Then the response HTTP status is 200
    And the suite response body has a "data" array

  Scenario: Admin creates a test case in a suite — returns 201
    Given I have created a suite named "Case Suite"
    When I create a test case titled "My Test Case" in that suite
    Then the response HTTP status is 201
    And the suite response body contains "title": "My Test Case"

  Scenario: TESTER cannot delete a suite — expects 403
    Given I have created a suite named "Protected Suite"
    When I log in as tester and DELETE that suite
    Then the response HTTP status is 403

  Scenario: Admin deletes a suite — returns 204
    Given I have created a suite named "Suite To Delete"
    When I delete that suite as admin
    Then the response HTTP status is 204
