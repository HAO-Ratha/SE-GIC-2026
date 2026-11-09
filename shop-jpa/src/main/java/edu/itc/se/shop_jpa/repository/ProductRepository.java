// ProductRepository.java
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsBySku(String sku);

    List<Product> findTop3ByCategoryOrderByPriceAmountDesc(Category category);

    List<Product> findByTagsName(String name);

    @Query(value = "SELECT p.* FROM product p WHERE p.stock < :min ORDER BY p.stock",
           nativeQuery = true)
    List<Product> findLowStock(@Param("min") int min);

    // Projection: see the note in Step 3
    @Query(value = """
        select new edu.itc.shop.web.dto.ProductView(p.id, p.sku, p.name, p.price.amount, p.stock)
          from Product p where p.category = :category
        """,
           countQuery = "select count(p) from Product p where p.category = :category")
    Page<ProductView> findByCategory(@Param("category") Category category, Pageable pageable);
}