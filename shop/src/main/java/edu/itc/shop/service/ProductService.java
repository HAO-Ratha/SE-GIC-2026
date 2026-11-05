
package edu.itc.shop.service;

import edu.itc.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    @Transactional
public void restock(long fromId, long toId, int qty) {
    if (qty <= 0) {
        throw new IllegalArgumentException(
            "Quantity must be positive"
        );
    }

    int sourceUpdated = repo.updateStock(fromId, -qty);
    if (sourceUpdated == 0) {
        throw new IllegalStateException(
            "Source product not found"
        );
    }

    int destinationUpdated = repo.updateStock(toId, qty);
    if (destinationUpdated == 0) {
        throw new IllegalStateException(
            "Destination product not found"
        );
    }
}
}
