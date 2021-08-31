package io.quarkiverse.cucumber.it.options;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;

@CucumberOptions(glue = { "io.quarkiverse.cucumber.it" }, tags = "@important", plugin = { "json" })
public class CucumberOptionsTest extends CucumberQuarkusTest {
    public static void main(String[] args) {
        runMain(CucumberOptionsTest.class, args);
    }
}
