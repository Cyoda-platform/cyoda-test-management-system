Feature: Attachment CRUD Operations

  Background:
    Given I am logged in as admin
    And I have valid bearer token
    And I have an attachment testing project with a suite and a case

  Scenario: Admin uploads a case attachment — returns 201 with CASE type
    When I upload a text file attachment to that case
    Then the response HTTP status is 201
    And the attachment response body contains "attachmentType": "CASE"

  Scenario: Admin can list all project attachments
    Given I have uploaded a text file attachment to that case
    When I GET all attachments for the project
    Then the response HTTP status is 200
    And the attachment response body has a "data" array

  Scenario: Admin deletes an attachment — returns 204
    Given I have uploaded a text file attachment to that case
    When I delete that attachment
    Then the response HTTP status is 204
