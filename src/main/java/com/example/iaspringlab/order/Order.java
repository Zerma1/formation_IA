package com.example.iaspringlab.order;

import com.example.iaspringlab.customer.Customer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(Customer customer) {
        this.customer = customer;
        this.status = OrderStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public void addLine(OrderLine line) {
        line.setOrder(this);
        lines.add(line);
    }

    public Optional<OrderLine> findLineForProduct(Long productId) {
        return lines.stream()
                .filter(line -> line.getProduct().getId().equals(productId))
                .findFirst();
    }

    public Optional<OrderLine> findLine(Long lineId) {
        return lines.stream()
                .filter(line -> line.getId().equals(lineId))
                .findFirst();
    }

    public void removeLine(OrderLine line) {
        lines.remove(line);
    }

    public boolean hasLines() {
        return !lines.isEmpty();
    }

    public BigDecimal total() {
        return lines.stream()
                .map(OrderLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
