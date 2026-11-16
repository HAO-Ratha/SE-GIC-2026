package edu.itc.shop;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {
    public static final String BASE_URI = "http://localhost:8080/api/";

    public static ResourceConfig configure() {
        return new ResourceConfig().packages("edu.itc.shop.web");
    }

    public static void main(String[] args) throws Exception {
        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), configure());
        System.out.println("shop-rest listening on " + BASE_URI);
        System.in.read();
        server.shutdown();
    }
}