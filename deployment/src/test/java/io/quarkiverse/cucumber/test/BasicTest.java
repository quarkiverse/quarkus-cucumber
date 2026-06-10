package io.quarkiverse.cucumber.test;

import io.quarkiverse.cucumber.CucumberOptions;
import io.quarkiverse.cucumber.CucumberQuarkusTest;

@CucumberOptions(glue = { "io.quarkiverse.cucumber.test" })
public class BasicTest extends CucumberQuarkusTest {

    public static void main(String[] args) {
        runMain(BasicTest.class, args);
    }

}
