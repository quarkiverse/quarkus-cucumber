package io.quarkiverse.cucumber.it;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path(CucumberResource.PATH)
@Produces(MediaType.TEXT_PLAIN)
public class CucumberResource {
    public static final String PATH = "cucumber";

    private final String greeting;

    public CucumberResource(@ConfigProperty(name = "greeting") String greeting) {
        this.greeting = greeting;
    }

    @GET
    public String hello() {
        return greeting;
    }
}
