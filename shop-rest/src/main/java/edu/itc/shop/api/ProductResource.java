package edu.itc.shop.api;

import edu.itc.shop.domain.Product;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {
    private final List<Product> products = new CopyOnWriteArrayList<>();

    public ProductResource() {
        products.add(new Product(1L, "Teclado", "TP-01", new BigDecimal("24.90"), 12, 1));
        products.add(new Product(2L, "Mouse", "MG-02", new BigDecimal("9.90"), 40, 1));
        products.add(new Product(3L, "T-shirt", "TS-03", new BigDecimal("39.90"), 8, 1));
    }

    @GET
    public List<Product> list() {
        return products;
    }

    @GET
    @Path("/{sku}")
    public Response get(@PathParam("sku") String sku) {
        return products.stream().filter(p -> p.sku().equals(sku))
                .findFirst().map(p -> Response.ok(p))
                .orElse(Response.status(Response.Status.NOT_FOUND)).build();
    }

    @POST
    public Response create(Product product) {
        products.add(product);
        return Response.status(Response.Status.CREATED).entity(product).build();
    }

    @PUT
    @Path("/{sku}")
    public Response update(@PathParam("sku") String sku, Product updatedProduct) {
        for (int i = 0; i < products.size(); i++) {
            if (products.get(i).sku().equals(sku)) {
                products.set(i, updatedProduct);
                return Response.ok(updatedProduct).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/{sku}")
    public Response delete(@PathParam("sku") String sku) {
        boolean removed = products.removeIf(p -> p.sku().equals(sku));
        return removed ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
    }
}