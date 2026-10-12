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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande.
 *
 * <p>Une commande appartient à exactement un client, contient au moins une ligne,
 * possède une quantité strictement positive par ligne et mémorise le prix du produit
 * au moment de la commande.</p>
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
    private OrderStatus status = OrderStatus.DRAFT;

    @Column(name = "total", precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
        // requis par JPA
    }

    public Order(Client client) {
        this.client = client;
    }

    /**
     * Ajoute une ligne en mémorisant le prix unitaire courant du produit.
     *
     * @throws BusinessRuleException si la quantité n'est pas strictement positive
     */
    public OrderLine addLine(Product product, int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("La quantité doit être strictement positive");
        }
        OrderLine line = new OrderLine(this, product, quantity, product.getPrice());
        lines.add(line);
        recalculateTotal();
        return line;
    }

    /**
     * Recalcule le total de la commande (somme des totaux de ligne).
     */
    public void recalculateTotal() {
        this.total = lines.stream()
                .map(OrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasLines() {
        return !lines.isEmpty();
    }

    /**
     * Applique une transition de statut en respectant les règles métier.
     *
     * @throws BusinessRuleException si la transition n'est pas autorisée
     *                               ou si la commande à confirmer n'a aucune ligne
     */
    public void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Transition de statut interdite : " + status + " -> " + target);
        }
        if (target == OrderStatus.CONFIRMED && !hasLines()) {
            throw new BusinessRuleException("Une commande doit contenir au moins une ligne pour être confirmée");
        }
        this.status = target;
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
        return lines;
    }
}
