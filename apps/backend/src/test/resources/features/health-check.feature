@e2e @tms @smoke
Feature: Application Health Check
  As an operations engineer
  I want a public health endpoint that needs no authentication
  So that load-balancers and monitors can verify the application is alive

  # -----------------------------------------------------------------------
  # These scenarios exercise the Spring Actuator health endpoint only.
  # No Cyoda entity operations are performed; the Spring context must start
  # successfully, but no live data is read or written.
  # Endpoint: GET /api/actuator/health  (context-path /api from application.yml)
  # Auth: public — explicitly whitelisted in AuthorizationFilter.isPublicEndpoint()
  # -----------------------------------------------------------------------

  Scenario: Health endpoint is reachable without an auth token
    When I call the health endpoint without authentication
    Then the health response HTTP status is 200

  Scenario: Health response body reports status UP
    When I call the health endpoint without authentication
    Then the health response body contains a "status" field
    And the health status value is "UP"

  Scenario: Health endpoint is not reachable at an unknown path
    When I call a non-existent path "/api/does-not-exist" without authentication
    Then the response HTTP status is 404
