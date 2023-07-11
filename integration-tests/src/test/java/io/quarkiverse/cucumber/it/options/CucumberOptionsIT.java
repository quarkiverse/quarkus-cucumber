package io.quarkiverse.cucumber.it.options;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusIntegrationTest;
import io.quarkiverse.cucumber.it.CucumberResourceIT;

@CucumberOptions(glue = { "io.quarkiverse.cucumber.it.steps" }, tags = "@important", plugin = { "json" })
@ApplicationScoped
public class CucumberOptionsIT extends CucumberQuarkusIntegrationTest {
    public static void main(String... args) {
        runMain(CucumberOptionsIT.class, args);
    }

    @Override
    protected Package[] packagesToScanRecursively() {
        return new Package[] { CucumberResourceIT.class.getPackage() };
    }
}