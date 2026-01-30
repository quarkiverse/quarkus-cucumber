package io.quarkiverse.cucumber;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;

import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;

/**
 * CDI event payload containing information about a Cucumber scenario.
 * <p>
 * This event is fired at the start and end of each scenario, qualified
 * with {@link BeforeScenario} or {@link AfterScenario} respectively.
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;ApplicationScoped
 * public class TestSetupObserver {
 *     public void onBeforeScenario(@Observes @BeforeScenario ScenarioEvent event) {
 *         System.out.println("Starting scenario: " + event.getName());
 *     }
 *
 *     public void onAfterScenario(@Observes @AfterScenario ScenarioEvent event) {
 *         if (event.isFailed()) {
 *             System.out.println("Scenario failed: " + event.getName());
 *         }
 *     }
 * }
 * </pre>
 *
 * @see BeforeScenario
 * @see AfterScenario
 */
public class ScenarioEvent {

    private final TestCase testCase;
    private final Status status;

    /**
     * Creates a new scenario event.
     *
     * @param testCase the Cucumber test case
     * @param status the current status (may be null for BeforeScenario events)
     */
    public ScenarioEvent(TestCase testCase, Status status) {
        this.testCase = Objects.requireNonNull(testCase, "testCase must not be null");
        this.status = status;
    }

    /**
     * Creates a new scenario event without status (for BeforeScenario).
     *
     * @param testCase the Cucumber test case
     */
    public ScenarioEvent(TestCase testCase) {
        this(testCase, null);
    }

    /**
     * Returns the name of the scenario.
     *
     * @return the scenario name
     */
    public String getName() {
        return testCase.getName();
    }

    /**
     * Returns the URI of the feature file containing this scenario.
     *
     * @return the feature file URI
     */
    public URI getUri() {
        return testCase.getUri();
    }

    /**
     * Returns the line number of the scenario in the feature file.
     *
     * @return the line number
     */
    public int getLine() {
        return testCase.getLocation().getLine();
    }

    /**
     * Returns the tags associated with this scenario.
     *
     * @return collection of tag names (e.g., "@smoke", "@regression")
     */
    public Collection<String> getTags() {
        return testCase.getTags();
    }

    /**
     * Returns the scenario execution status.
     * <p>
     * Note: This is only populated for {@link AfterScenario} events.
     * For {@link BeforeScenario} events, this returns {@code null}.
     *
     * @return the execution status, or null if not yet executed
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Checks if the scenario has failed.
     * <p>
     * This is a convenience method equivalent to checking if
     * {@link #getStatus()} equals {@link Status#FAILED}.
     *
     * @return true if the scenario failed, false otherwise
     */
    public boolean isFailed() {
        return status == Status.FAILED;
    }

    /**
     * Checks if the scenario has passed.
     * <p>
     * This is a convenience method equivalent to checking if
     * {@link #getStatus()} equals {@link Status#PASSED}.
     *
     * @return true if the scenario passed, false otherwise
     */
    public boolean isPassed() {
        return status == Status.PASSED;
    }

    /**
     * Returns the underlying Cucumber TestCase.
     * <p>
     * Use this for advanced scenarios where you need access to
     * the full Cucumber API.
     *
     * @return the Cucumber test case
     */
    public TestCase getTestCase() {
        return testCase;
    }

    @Override
    public String toString() {
        return "ScenarioEvent{" +
                "name='" + getName() + '\'' +
                ", uri=" + getUri() +
                ", line=" + getLine() +
                ", status=" + status +
                '}';
    }
}
