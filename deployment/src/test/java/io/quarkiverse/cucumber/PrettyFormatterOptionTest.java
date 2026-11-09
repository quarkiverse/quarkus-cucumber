package io.quarkiverse.cucumber;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link CucumberOptions#pretty()}: the {@code PrettyFormatter} stays on unless explicitly disabled.
 */
class PrettyFormatterOptionTest {

    @CucumberOptions
    static class DefaultOptions {
    }

    @CucumberOptions(pretty = false)
    static class PrettyDisabled {
    }

    @Test
    void enabledWithoutCucumberOptions() {
        assertTrue(CucumberQuarkusTest.isPrettyFormatterEnabled(null));
    }

    @Test
    void enabledByDefault() {
        assertTrue(CucumberQuarkusTest.isPrettyFormatterEnabled(DefaultOptions.class));
    }

    @Test
    void disabledWithPrettyFalse() {
        assertFalse(CucumberQuarkusTest.isPrettyFormatterEnabled(PrettyDisabled.class));
    }
}
