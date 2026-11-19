
package edu.itc.shop;

import edu.itc.shop.api.ProductResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static final String BASE_URI = "http://localhost:8080/api/";

    public static ResourceConfig configure() {
        return new ResourceConfig().register(new ProductResource());
    }

    public static void main(String[] args) throws Exception {
        final HttpServer server =
                GrizzlyHttpServerFactory.createHttpServer(
                        URI.create(BASE_URI), configure());

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

        System.out.println("shop-rest listening on " + BASE_URI);

        new CountDownLatch(1).await();
    }
}
