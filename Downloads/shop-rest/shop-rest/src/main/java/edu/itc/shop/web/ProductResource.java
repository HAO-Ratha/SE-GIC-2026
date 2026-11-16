package edu.itc.shop.web;

import edu.itc.shop.domain.Product;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {
    private final List<Product> products = new ArrayList<>();

    public ProductResource() {
        products.add(new Product("TP-01", "Teclado", new BigDecimal("24.90"), "HARDWARE", 12));
        products.add(new Product("MG-02", "Mouse", new BigDecimal("9.90"), "HARDWARE", 40));
        products.add(new Product("TS-03", "T-shirt", new BigDecimal("39.90"), "CLOTHING", 8));
    }

    @GET
    public List<Product> list() {
        return products;
    }

    @GET
    @Path("/{sku}")
    public Response get(@PathParam("sku") String sku) {
        return products.stream().filter(p -> p.getSku().equals(sku))
                .findFirst().map(Response::ok)
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
            if (products.get(i).getSku().equals(sku)) {
                products.set(i, updatedProduct);
                return Response.ok(updatedProduct).build();
            }
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @DELETE
    @Path("/{sku}")
    public Response delete(@PathParam("sku") String sku) {
        boolean removed = products.removeIf(p -> p.getSku().equals(sku));
        return removed ? Response.noContent().build() : Response.status(Response.Status.NOT_FOUND).build();
    }
}