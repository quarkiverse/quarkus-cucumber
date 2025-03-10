Feature: That should not be run if subpackage is declared
  @important
  Scenario: That does nothing unless it's run from CucumberOptionsTest
    And controlled failure
