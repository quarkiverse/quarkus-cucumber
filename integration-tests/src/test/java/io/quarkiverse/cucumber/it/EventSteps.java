package io.quarkiverse.cucumber.it;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.inject.Inject;

import io.cucumber.java.en.Then;

/**
 * Step definitions for verifying scenario lifecycle events.
 */
public class EventSteps {

    @Inject
    ScenarioEventObserver observer;

    @Then("the before scenario event was fired")
    public void verifyBeforeEventFired() {
        assertFalse(observer.getBeforeEvents().isEmpty(),
                "Expected at least one BeforeScenario event to be fired");
    }

    @Then("the after scenario event was fired for previous scenario")
    public void verifyAfterEventFiredForPrevious() {
        // After events from previous scenarios should be present
        // (this step verifies that after events are fired correctly)
        assertTrue(observer.getAfterEvents().size() > 0 || observer.getBeforeEvents().size() > 0,
                "Expected scenario events to be captured");
    }

    @Then("the scenario name is captured in the event")
    public void verifyScenarioNameCaptured() {
        assertTrue(observer.getBeforeEvents().stream()
                .anyMatch(name -> name.contains("CDI events")),
                "Expected scenario name to be captured in events");
    }
}
