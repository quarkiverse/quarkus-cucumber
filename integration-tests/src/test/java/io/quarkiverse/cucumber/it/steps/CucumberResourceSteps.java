package io.quarkiverse.cucumber.it.steps;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.quarkiverse.cucumber.it.actors.CucumberResourceActor;

@ApplicationScoped
public class CucumberResourceSteps {
    private final CucumberResourceActor actor;

    @Inject
    public CucumberResourceSteps(CucumberResourceActor actor) {
        this.actor = actor;
    }

    @Given("^print \"(.+)\"$")
    public void print(String message) {
        System.out.println(message);
    }

    @Given("I call the endpoint")
    public void i_call_endpoint() {
        actor.callTarget();
    }

    @Then("the response is ok")
    public void response_is_ok() {
        actor.verifyResponse(Response.Status.OK.getStatusCode());
    }
}
