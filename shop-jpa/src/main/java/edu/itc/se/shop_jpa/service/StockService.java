package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.repository.ProductRepository;
import jakarta.persistence.OptimisticLockException;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private static final int MAX_OPTIMISTIC_ATTEMPTS = 100;

    private final ProductRepository products;
    private final TransactionTemplate optimisticTransaction;
    private final AtomicLong optimisticRetries = new AtomicLong();

    public StockService(ProductRepository products, PlatformTransactionManager transactionManager) {
        this.products = products;
        this.optimisticTransaction = new TransactionTemplate(transactionManager);
        this.optimisticTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public void reserve(Long productId, int quantity) {
        validate(productId, quantity);
        Product product = products.findForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (product.getStock() < quantity) {
            throw new OutOfStockException(productId, quantity);
        }
        product.setStock(product.getStock() - quantity);
    }

    public void reserveOptimistically(Long productId, int quantity) {
        validate(productId, quantity);
        for (int attempt = 1; attempt <= MAX_OPTIMISTIC_ATTEMPTS; attempt++) {
            try {
                optimisticTransaction.executeWithoutResult(status -> {
                    Product product = products.findById(productId)
                            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
                    if (product.getStock() < quantity) {
                        throw new OutOfStockException(productId, quantity);
                    }
                    product.setStock(product.getStock() - quantity);
                });
                return;
            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {
                optimisticRetries.incrementAndGet();
                if (attempt == MAX_OPTIMISTIC_ATTEMPTS) {
                    throw new IllegalStateException("Optimistic reservation retry limit reached", ex);
                }
                Thread.onSpinWait();
            }
        }
    }

    @Transactional
    public void reserveAtomically(Long productId, int quantity) {
        validate(productId, quantity);
        if (products.reserveStockAtomically(productId, quantity) == 0) {
            throw new OutOfStockException(productId, quantity);
        }
    }

    public long optimisticRetryCount() {
        return optimisticRetries.get();
    }

    public void resetOptimisticRetryCount() {
        optimisticRetries.set(0);
    }

    private static void validate(Long productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("productId must not be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
