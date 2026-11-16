package edu.itc.shop.domain;

import java.math.BigDecimal;

public class Product {
    private String sku;
    private String name;
    private BigDecimal price;
    private String category;
    private int stock;
    private int version;

    public Product() {}

    public Product(String sku, String name, BigDecimal price, String category, int stock) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.category = category;
        this.stock = stock;
        this.version = 1;
    }

    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getCategory() { return category; }
    public int getStock() { return stock; }
    public int getVersion() { return version; }
}