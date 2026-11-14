package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Session;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orders;

    @PersistenceContext
    private EntityManager entityManager;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    public Map<Long, Integer> linesPerPaidOrder() {
        entityManager.unwrap(Session.class).setFetchBatchSize(1);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Order order : orders.findByStatus(OrderStatus.PAID)) {
            result.put(order.getId(), order.getLines().size());
        }
        return result;
    }

    public Map<Long, Integer> linesPerPaidOrderJoinFetch() {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Order order : orders.findByStatusWithLines(OrderStatus.PAID)) {
            result.put(order.getId(), order.getLines().size());
        }
        return result;
    }

    public Map<Long, Integer> linesPerPaidOrderEntityGraph() {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Order order : orders.findWithGraphByStatus(OrderStatus.PAID)) {
            result.put(order.getId(), order.getLines().size());
        }
        return result;
    }

    public Map<Long, Integer> linesPerPaidOrderBatched() {
        entityManager.unwrap(Session.class).setFetchBatchSize(16);
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (Order order : orders.findByStatus(OrderStatus.PAID)) {
            result.put(order.getId(), order.getLines().size());
        }
        return result;
    }

    public Map<Long, List<String>> lineNamesPerPaidOrder() {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        for (Order order : orders.findByStatus(OrderStatus.PAID)) {
            result.put(order.getId(), order.getLines().stream()
                    .map(line -> line.getProduct().getName())
                    .toList());
        }
        return result;
    }
}
