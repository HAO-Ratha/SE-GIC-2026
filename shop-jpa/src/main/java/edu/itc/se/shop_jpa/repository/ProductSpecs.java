package edu.itc.se.shop_jpa.repository;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Money;
import edu.itc.se.shop_jpa.domain.Product;

public final class ProductSpecs {
    private ProductSpecs() { }

    public static Specification<Product> nameContains(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.<String>get("name")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Product> inCategory(Category c) {
        return (root, query, cb) -> cb.equal(root.get("category"), c);
    }

    public static Specification<Product> cheaperThan(BigDecimal max) {
        return (root, query, cb) ->
                cb.le(root.<Money>get("price").<BigDecimal>get("amount"), max);
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.<Integer>get("stock"), 0);
    }

    public static Specification<Product> filter(String q, Category c, BigDecimal max, Boolean inStock) {
        return Specification.allOf(
                q == null || q.isBlank() ? null : nameContains(q),
                c == null ? null : inCategory(c),
                max == null ? null : cheaperThan(max),
                Boolean.TRUE.equals(inStock) ? inStock() : null);
    }
}