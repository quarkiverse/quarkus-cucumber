Feature: Scenario lifecycle events

  Scenario: CDI events are fired for scenarios
    Given I call the endpoint
    Then the response is ok
    And the before scenario event was fired
    And the scenario name is captured in the event
