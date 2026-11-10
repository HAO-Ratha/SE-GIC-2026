package edu.itc.se.shop_jpa.service;

import edu.itc.se.shop_jpa.domain.Order;
import edu.itc.se.shop_jpa.domain.OrderStatus;
import edu.itc.se.shop_jpa.repository.OrderRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final OrderRepository orders;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    public Map<Long, Integer> linesPerPaidOrder() {
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
