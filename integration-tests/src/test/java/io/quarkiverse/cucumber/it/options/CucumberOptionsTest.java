package io.quarkiverse.cucumber.it.options;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;
import io.quarkiverse.cucumber.it.Steps;

@CucumberOptions(glue = { "io.quarkiverse.cucumber.it" }, tags = "@important", plugin = { "json" })
public class CucumberOptionsTest extends CucumberQuarkusTest {
    @BeforeEach
    public void before() {
        Steps.enableControlledFailure();
    }

    @AfterEach
    public void after() {
        Steps.disableControlledFailure();
    }

    public static void main(String[] args) {
        runMain(CucumberOptionsTest.class, args);
    }
}
