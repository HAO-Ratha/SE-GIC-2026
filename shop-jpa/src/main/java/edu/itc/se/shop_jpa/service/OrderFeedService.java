package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import edu.itc.se.shop_jpa.web.dto.OrderFeedItem;
import edu.itc.se.shop_jpa.web.dto.OrderFeedResponse;
import edu.itc.se.shop_jpa.web.dto.OrderSummary;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderFeedService {

    private static final int MAX_SIZE = 100;
    private static final Sort FEED_SORT = Sort.by(
            Sort.Order.desc("placedAt"), Sort.Order.desc("id"));

    private final OrderRepository orders;

    public OrderFeedService(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public OrderFeedResponse slice(String after, int size) {
        validateSize(size);
        PageRequest request = PageRequest.of(0, size, FEED_SORT);
        Slice<OrderSummary> slice;
        if (after == null || after.isBlank()) {
            slice = orders.findAllBy(request);
        } else {
            Cursor cursor = parseCursor(after);
            slice = orders.findFeedAfter(cursor.placedAt(), cursor.id(), PageRequest.of(0, size));
        }
        List<OrderFeedItem> items = slice.getContent().stream().map(OrderFeedService::item).toList();
        return new OrderFeedResponse(items, slice.hasNext() ? cursor(items.getLast()) : null, slice.hasNext());
    }

    @Transactional(readOnly = true)
    public OrderFeedResponse keyset(String after, int size) {
        validateSize(size);
        ScrollPosition position = after == null || after.isBlank()
                ? ScrollPosition.keyset()
                : toPosition(parseCursor(after));
        Window<Order> window = orders.findBy(
                (root, query, cb) -> cb.conjunction(),
                query -> query.sortBy(FEED_SORT).limit(size).scroll(position));
        List<OrderFeedItem> items = window.getContent().stream().map(OrderFeedService::item).toList();
        String nextCursor = null;
        if (window.hasNext() && !window.isEmpty()) {
            Order last = window.getContent().getLast();
            KeysetScrollPosition nextPosition = (KeysetScrollPosition) window.positionAt(last);
            Map<String, Object> keys = nextPosition.getKeys();
            Object placedAt = keys.get("placedAt");
            Object id = keys.get("id");
            nextCursor = placedAt + "," + id;
        }
        return new OrderFeedResponse(items, nextCursor, window.hasNext());
    }

    private static ScrollPosition toPosition(Cursor cursor) {
        return ScrollPosition.forward(Map.of("placedAt", cursor.placedAt(), "id", cursor.id()));
    }

    private static Cursor parseCursor(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("after must be <placedAt>,<id>");
        }
        try {
            return new Cursor(Instant.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("after must contain an ISO-8601 timestamp and numeric id", ex);
        }
    }

    private static void validateSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
    }

    private static OrderFeedItem item(OrderSummary order) {
        return new OrderFeedItem(order.getId(), order.getStatus(), order.getPlacedAt());
    }

    private static OrderFeedItem item(Order order) {
        return new OrderFeedItem(order.getId(), order.getStatus(), order.getPlacedAt());
    }

    private static String cursor(OrderFeedItem item) {
        return item.placedAt() + "," + item.id();
    }

    private record Cursor(Instant placedAt, Long id) {
    }
}
