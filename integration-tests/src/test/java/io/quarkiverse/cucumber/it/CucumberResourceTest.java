package io.quarkiverse.cucumber.it;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;

@CucumberOptions(glue = { "io.quarkiverse.cucumber.it" })
public class CucumberResourceTest extends CucumberQuarkusTest {
    public static void main(String[] args) {
        runMain(CucumberResourceTest.class, args);
    }
}
