Feature: Test feature

  Scenario: Test scenario
    Given I call the endpoint
    Then the response is ok

  Scenario: Test initialized stateful scenario
    Given the state "ok"
    Then the state is "ok"

  Scenario: Test non initialized stateful scenario
    Then the state is not initialized
