package edu.itc.se.shop_jpa.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    // Derived queries
    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
    List<Order> findByStatus(OrderStatus status);
    long countByStatus(OrderStatus status);

    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);

    // Interface projection (only id, status, placedAt, customer.name are selected)
    Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);

    Slice<OrderSummary> findAllBy(Pageable pageable);

    @Query("""
            select o from Order o
             where o.placedAt < :placedAt
                or (o.placedAt = :placedAt and o.id < :id)
             order by o.placedAt desc, o.id desc
            """)
    Slice<OrderSummary> findFeedAfter(
            @Param("placedAt") Instant placedAt, @Param("id") Long id, Pageable pageable);

    @Query("""
        select distinct o from Order o
          join fetch o.lines l
          join fetch l.product
         where o.status = :status
        """)
    List<Order> findByStatusWithLines(@Param("status") OrderStatus status);

    @EntityGraph(attributePaths = {"lines", "lines.product"})
    @Query("select o from Order o where o.status = :status")
    List<Order> findWithGraphByStatus(@Param("status") OrderStatus status);

    // JPQL with join fetch
    @Query("""
        select distinct o from Order o
          join fetch o.lines l
          join fetch l.product
         where o.customer.email = :email
         order by o.placedAt desc
        """)
    List<Order> findWithLinesByCustomerEmail(@Param("email") String email);

    // JPQL aggregate
    @Query("select count(l) from OrderLine l where l.order.status = :status")
    long countLinesByStatus(@Param("status") OrderStatus status);
}