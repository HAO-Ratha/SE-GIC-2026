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

class ProductNotFoundTests {

    private HttpServer server;
    private HttpClient client;
    private URI productsUri;

    @BeforeEach
    void startServer() {
        server = GrizzlyHttpServerFactory.createHttpServer(
                URI.create("http://localhost:0/api/"), Main.configure());
        int port = server.getListener("grizzly").getPort();
        productsUri = URI.create("http://localhost:" + port + "/api/products");
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void unknownProductReturnsNotFound() throws Exception {
        HttpResponse<String> get = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/UNKNOWN")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, get.statusCode());

        String body = """
                {"sku":"UNKNOWN","name":"Unknown","price":1.00,"category":"TEST","stock":1,"version":1}
                """;
        HttpResponse<String> update = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/UNKNOWN"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, update.statusCode());

        HttpResponse<String> delete = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/UNKNOWN")).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, delete.statusCode());
    }
}
