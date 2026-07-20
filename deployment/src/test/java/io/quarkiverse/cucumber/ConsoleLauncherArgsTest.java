package io.quarkiverse.cucumber;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Regression coverage for #298: the IDE/CLI runner must pass the {@code execute} subcommand to the
 * JUnit Platform 6 {@code ConsoleLauncher} that Quarkus 3.31+ ships. The tests run on that classpath.
 */
class ConsoleLauncherArgsTest {

    @Test
    void detectsJUnitPlatform6OnTheTestClasspath() {
        assertTrue(CucumberQuarkusTest.isJUnitPlatform6OrLater(),
                "Quarkus 3.31+ ships JUnit Platform 6, so it must be detected on the test classpath");
    }

    @Test
    void prependsExecuteSubcommandForJUnit6() {
        assertArrayEquals(new String[] { "execute", "-c", "com.example.MyTest" },
                CucumberQuarkusTest.consoleLauncherArgs("com.example.MyTest"));
    }
}
