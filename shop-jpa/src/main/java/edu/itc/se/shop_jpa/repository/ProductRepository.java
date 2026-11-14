package edu.itc.se.shop_jpa.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import edu.itc.se.shop_jpa.domain.Category;
import edu.itc.se.shop_jpa.domain.Product;
import edu.itc.se.shop_jpa.web.dto.ProductView;

public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product>,
        ProductReportRepository {

       // Derived queries
       boolean existsBySku(String sku);
       List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);

    List<Product> findByTagsName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
               set p.stock = p.stock - :qty, p.version = p.version + 1
             where p.id = :id and p.stock >= :qty
            """)
    int reserveStockAtomically(@Param("id") Long id, @Param("qty") int qty);

    // Native query
    @Query(value = "SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock",
           nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);

    // Record projection via constructor expression (price.amount -> amount)
    @Query(value = """
            select new edu.itc.se.shop_jpa.web.dto.ProductView(p.id, p.sku, p.name, p.price.amount, p.stock)
              from Product p where p.category = :category
            """,
           countQuery = "select count(p) from Product p where p.category = :category")
    Page<ProductView> findByCategory(@Param("category") Category category, Pageable pageable);
}