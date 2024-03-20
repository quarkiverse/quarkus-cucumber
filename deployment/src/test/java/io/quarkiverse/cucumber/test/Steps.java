package io.quarkiverse.cucumber.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.response.ValidatableResponse;

public class Steps {

    @Inject
    @ConfigProperty(defaultValue = "/")
    String target;

    private ValidatableResponse result;

    @BeforeAll
    public static void beforeAll() {
        Factory.setJusteHello(true);
    }

    @AfterAll
    public static void afterAll() {
        Factory.setJusteHello(false);
        String response = given()
                .when().get("/")
                .then()
                .extract().body().asString();
        assertFalse("hello".equals(response));
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

    @Then("the response body is hello")
    public void response_body_is_hello() throws Exception {
        result.body(is("hello"));
    }

}
