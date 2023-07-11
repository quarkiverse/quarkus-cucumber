package io.quarkiverse.cucumber.test;

import static io.restassured.RestAssured.given;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.ValidatableResponse;

public class Steps {
    private final String target;

    @Inject
    public Steps(@ConfigProperty(name = "some.endpoint.path", defaultValue = "/") String target) {
        this.target = target;
    }

    private ValidatableResponse result;

    @Given("I call the endpoint")
    public void i_call_endpoint() {
        result = given()
                .when().get(target)
                .then();
    }

    @Then("the response is ok")
    public void response_is_ok() {
        result.statusCode(200);
    }
}