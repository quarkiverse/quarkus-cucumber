package io.quarkiverse.cucumber.it;

import static io.restassured.RestAssured.given;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.ValidatableResponse;

public class Steps {
    private static boolean fail = false;

    @Inject
    @ConfigProperty(name = "testPath", defaultValue = "/")
    String target;

    private ValidatableResponse result;

    @Given("^print \"(.+)\"$")
    public void print(String message) throws Exception {
        System.out.println(message);
    }

    @Given("I call the endpoint")
    public void i_call_endpoint() throws Exception {
        result = given()
                .when().get(target)
                .then();
    }

    @Then("the response is ok")
    public void response_is_ok() throws Exception {
        result.statusCode(200);
    }

    @And("controlled failure")
    public void controlled_failure() throws Exception {
        if (fail) {
            throw new AssertionError("Unexpected failure");
        }
    }

    public static void enableControlledFailure() {
        fail = true;
    }

    public static void disableControlledFailure() {
        fail = false;
    }
}
