package io.quarkiverse.cucumber.it;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkiverse.cucumber.AfterScenario;
import io.quarkiverse.cucumber.BeforeScenario;
import io.quarkiverse.cucumber.ScenarioEvent;

/**
 * Test observer that captures scenario lifecycle events for verification.
 */
@ApplicationScoped
public class ScenarioEventObserver {

    private final List<String> beforeEvents = new CopyOnWriteArrayList<>();
    private final List<String> afterEvents = new CopyOnWriteArrayList<>();

    public void onBeforeScenario(@Observes @BeforeScenario ScenarioEvent event) {
        beforeEvents.add(event.getName());
    }

    public void onAfterScenario(@Observes @AfterScenario ScenarioEvent event) {
        afterEvents.add(event.getName() + ":" + event.getStatus());
    }

    public List<String> getBeforeEvents() {
        return beforeEvents;
    }

    public List<String> getAfterEvents() {
        return afterEvents;
    }

    public void clear() {
        beforeEvents.clear();
        afterEvents.clear();
    }
}
