package io.quarkiverse.cucumber.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class Endpoint {

    @GET
    public String hello() {
        return "hello";
    }
}
