package edu.itc.shop.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class ShopOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "placed_at", nullable = false)
    private Instant placedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected ShopOrder() {}

    public ShopOrder(Customer customer) {
        this.customer = customer;
        this.status = OrderStatus.NEW;
        this.placedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public Instant getPlacedAt() { return placedAt; }
    public List<OrderLine> getLines() { return lines; }

    // Task 2 methods
    public void addLine(Product p, int qty) {
        OrderLine line = new OrderLine(p, qty);
        line.setOrder(this);
        this.lines.add(line);
    }

    public void removeLine(OrderLine line) {
        this.lines.remove(line);
        line.setOrder(null);
    }
}