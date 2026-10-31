package edu.itc.shop.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.itc.shop.service.ProductService;

@Configuration
public class AppRunner {

    @Bean
    public CommandLineRunner run(ProductService productService) {
        return args -> {
            System.out.println("=== Testing Database ===");
            
            // 1. Successful transaction
            System.out.println("Attempting to save 2 products successfully...");
            productService.createTwoProductsWithTransaction("Laptop", "Mouse");
            System.out.println("List of products in DB: " + productService.getAllProducts());
            
            // 2. Failed transaction (rollback test)
            System.out.println("\nAttempting to save 2 products, but the second one is empty...");
            try {
                productService.createTwoProductsWithTransaction("Keyboard", "");
            } catch (Exception e) {
                System.out.println("Caught Exception: " + e.getMessage());
            }
            
            // 3. Verify the rollback
            System.out.println("\nList of products after failed transaction (should ONLY have Laptop and Mouse):");
            System.out.println(productService.getAllProducts());
        };
    }
}