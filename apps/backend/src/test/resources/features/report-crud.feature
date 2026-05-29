Feature: Report CRUD Operations

  Background:
    Given I am logged in as admin
    And I have valid bearer token
    And I have a report testing project

  Scenario: Admin creates a report — returns 201 with required fields
    When I create a report named "Sprint 1 Summary"
    Then the response HTTP status is 201
    And the report response body contains "name": "Sprint 1 Summary"

  Scenario: Admin can retrieve a report by ID
    Given I have created a report named "Retrievable Report"
    When I GET that report by ID
    Then the response HTTP status is 200
    And the report response body contains "name": "Retrievable Report"

  Scenario: Admin can get all reports for the project
    Given I have created a report named "Listed Report"
    When I GET all reports for the project
    Then the response HTTP status is 200
    And the report response body has a "data" array

  Scenario: Admin deletes a report — returns 204
    Given I have created a report named "Report To Delete"
    When I delete that report
    Then the response HTTP status is 204
