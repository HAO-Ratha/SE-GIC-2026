package edu.itc.shop;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopRestApplicationTests {

    private HttpServer server;
    private HttpClient client;
    private URI baseUri;

    @BeforeEach
    void startServer() {
        server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create("http://localhost:0/api/"), Main.configure());
        int port = server.getListener("grizzly").getPort();
        baseUri = URI.create("http://localhost:" + port + "/api/products");
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void catalogListsSeededProducts() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(baseUri).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("TP-01"));
        assertTrue(response.body().contains("MG-02"));
        assertTrue(response.body().contains("TS-03"));
    }
}
