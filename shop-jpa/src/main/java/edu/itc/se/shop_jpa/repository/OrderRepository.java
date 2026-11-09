// OrderRepository.java
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);

    long countByStatus(OrderStatus status);

    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);

    Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);

    @Query("""
        select distinct o from Order o
          join fetch o.lines l
          join fetch l.product
         where o.customer.email = :email
         order by o.placedAt desc
        """)
    List<Order> findWithLinesByCustomerEmail(@Param("email") String email);

    @Query("select count(l) from OrderLine l where l.order.status = :status")
    long countLinesByStatus(@Param("status") OrderStatus status);
}