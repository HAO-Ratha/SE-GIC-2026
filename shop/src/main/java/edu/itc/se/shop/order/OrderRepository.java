package edu.itc.se.shop.order;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {

    private static final String ORDER_COLUMNS = "id, customer_id, status, placed_at, total";

    private static final RowMapper<Order> ORDER_MAPPER = (rs, i) -> new Order(
            rs.getLong("id"),
            rs.getLong("customer_id"),
            OrderStatus.valueOf(rs.getString("status")),
            rs.getObject("placed_at", LocalDateTime.class),
            rs.getBigDecimal("total"));

    private record LineRow(long orderId, OrderLine line) {}

    private record Filter(String where, Map<String, Object> params) {}

    private final JdbcClient jdbc;

    public OrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts the order row and all its lines; returns the generated id. Runs inside the caller's transaction. */
    public long insert(Order order, List<OrderLine> lines) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO orders (customer_id, status, placed_at, total) "
                        + "VALUES (:customerId, :status, :placedAt, :total)")
                .param("customerId", order.customerId())
                .param("status", order.status().name())
                .param("placedAt", order.placedAt())
                .param("total", order.total())
                .update(keys, "id");
        long id = keys.getKey().longValue();

        for (OrderLine l : lines) {
            jdbc.sql("INSERT INTO order_line (order_id, product_id, qty, unit_price) "
                            + "VALUES (:orderId, :productId, :qty, :unitPrice)")
                    .param("orderId", id)
                    .param("productId", l.productId())
                    .param("qty", l.qty())
                    .param("unitPrice", l.unitPrice())
                    .update();
        }
        return id;
    }

    public Optional<Order> findById(long id) {
        return jdbc.sql("SELECT " + ORDER_COLUMNS + " FROM orders WHERE id = :id")
                .param("id", id).query(ORDER_MAPPER).optional();
    }

    public List<OrderLine> findLines(long orderId) {
        return jdbc.sql("SELECT product_id, qty, unit_price FROM order_line "
                        + "WHERE order_id = :id ORDER BY product_id")
                .param("id", orderId)
                .query((rs, i) -> new OrderLine(rs.getLong("product_id"), rs.getInt("qty"), rs.getBigDecimal("unit_price")))
                .list();
    }

    /** One query for the lines of many orders (avoids N+1 when listing). */
    public Map<Long, List<OrderLine>> findLinesByOrderIds(Collection<Long> orderIds) {
        Map<Long, List<OrderLine>> result = new LinkedHashMap<>();
        if (orderIds.isEmpty()) return result;
        List<LineRow> rows = jdbc.sql("SELECT order_id, product_id, qty, unit_price FROM order_line "
                        + "WHERE order_id IN (:ids) ORDER BY order_id, product_id")
                .param("ids", orderIds)
                .query((rs, i) -> new LineRow(rs.getLong("order_id"),
                        new OrderLine(rs.getLong("product_id"), rs.getInt("qty"), rs.getBigDecimal("unit_price"))))
                .list();
        for (LineRow r : rows) {
            result.computeIfAbsent(r.orderId(), k -> new java.util.ArrayList<>()).add(r.line());
        }
        return result;
    }

    public List<Order> find(Long customerId, OrderStatus status, int limit, long offset) {
        Filter f = filter(customerId, status);
        return jdbc.sql("SELECT " + ORDER_COLUMNS + " FROM orders" + f.where()
                        + " ORDER BY id DESC LIMIT :limit OFFSET :offset")
                .params(f.params())
                .param("limit", limit)
                .param("offset", offset)
                .query(ORDER_MAPPER)
                .list();
    }

    public long count(Long customerId, OrderStatus status) {
        Filter f = filter(customerId, status);
        return jdbc.sql("SELECT COUNT(*) FROM orders" + f.where())
                .params(f.params())
                .query(Long.class)
                .single();
    }

    /** Compare-and-set on the status: returns 0 if the order is no longer in the {@code from} state. */
    public int updateStatus(long id, OrderStatus from, OrderStatus to) {
        return jdbc.sql("UPDATE orders SET status = :to WHERE id = :id AND status = :from")
                .param("to", to.name())
                .param("id", id)
                .param("from", from.name())
                .update();
    }

    // Only constant SQL fragments are concatenated; every value is a bound parameter.
    private static Filter filter(Long customerId, OrderStatus status) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (customerId != null) {
            where.append(" AND customer_id = :customerId");
            params.put("customerId", customerId);
        }
        if (status != null) {
            where.append(" AND status = :status");
            params.put("status", status.name());
        }
        return new Filter(where.toString(), params);
    }
}