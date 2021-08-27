package io.quarkiverse.cucumber.it;

import javax.inject.Inject;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.ValidatableResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import static io.restassured.RestAssured.given;

public class Steps {

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

}
