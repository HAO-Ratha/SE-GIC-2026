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

class ProductCrudTests {

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
    void createsUpdatesAndDeletesAProduct() throws Exception {
        String body = """
                {"sku":"TEST-01","name":"Test product","price":12.50,"category":"TEST","stock":3,"version":1}
                """;

        HttpResponse<String> create = client.send(
                HttpRequest.newBuilder(productsUri)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(201, create.statusCode());

        HttpResponse<String> read = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/TEST-01")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, read.statusCode());
        assertTrue(read.body().contains("Test product"));

        String updated = """
                {"sku":"TEST-01","name":"Updated product","price":15.00,"category":"TEST","stock":5,"version":2}
                """;
        HttpResponse<String> update = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/TEST-01"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(updated))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, update.statusCode());
        assertTrue(update.body().contains("Updated product"));

        HttpResponse<String> delete = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/TEST-01")).DELETE().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(204, delete.statusCode());

        HttpResponse<String> missing = client.send(
                HttpRequest.newBuilder(URI.create(productsUri + "/TEST-01")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, missing.statusCode());
    }
}
