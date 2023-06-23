package io.quarkiverse.cucumber.it.actors;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkiverse.cucumber.it.CucumberResource;
import io.restassured.response.ValidatableResponse;

@ApplicationScoped
public class CucumberResourceActor {
    private final String greeting;

    @Inject
    public CucumberResourceActor(@ConfigProperty(name = "greeting") String greeting) {
        this.greeting = greeting;
    }

    private ValidatableResponse response;

    public void callTarget() {
        response = when().get(CucumberResource.PATH)
                .then();
    }

    public void verifyResponse(int code) {
        try {
            response.statusCode(code)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(is(greeting));
        } finally {
            resetState();
        }
    }

    private void resetState() {
        response = null;
    }
}
