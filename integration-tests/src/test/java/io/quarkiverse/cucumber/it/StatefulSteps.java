package io.quarkiverse.cucumber.it;

import java.util.Objects;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class StatefulSteps {

    String state;

    @Given("the state {string}")
    public void theState(String state) {
        this.state = state;
    }

    @Then("the state is not initialized")
    public void theStateIsNotInitialized() {
        assert state == null;
    }

    @Then("the state is {string}")
    public void theStateIs(String state) {
        assert Objects.equals(this.state, state);
    }
}
