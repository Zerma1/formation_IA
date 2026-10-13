package com.example.iaspringlab.domain;

import com.example.iaspringlab.exception.BusinessRuleException;
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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer order.
 *
 * <p>An order belongs to exactly one customer, contains at least one line, has a
 * strictly positive quantity per line and keeps a snapshot of the product price.
 * The total is the sum of {@code quantity × unitPrice} over all lines.</p>
 */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
        // required by JPA
    }

    public Order(Client client) {
        this.client = client;
        this.status = OrderStatus.DRAFT;
        this.total = BigDecimal.ZERO;
    }

    /**
     * Adds a line for the given product and quantity, then recomputes the total.
     *
     * @throws BusinessRuleException when the quantity is not strictly positive.
     */
    public void addLine(Product product, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("La quantité d'une ligne doit être strictement positive");
        }
        lines.add(new OrderLine(this, product, quantity));
        recalculateTotal();
    }

    /**
     * Moves the order to {@code target}.
     *
     * @throws BusinessRuleException when the transition is not allowed.
     */
    public void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Transition de statut interdite : " + status + " -> " + target);
        }
        this.status = target;
    }

    private void recalculateTotal() {
        this.total = lines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<OrderLine> getLines() {
        return List.copyOf(lines);
    }
}
