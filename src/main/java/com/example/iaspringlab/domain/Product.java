package com.example.iaspringlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * Product that can be ordered.
 *
 * <p>A product has a reference, a name, a price and an active status. An inactive
 * product cannot be added to an order.</p>
 */
@Entity
@Table(name = "product", uniqueConstraints = @UniqueConstraint(name = "uk_product_reference", columnNames = "reference"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false)
    private String reference;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Product() {
        // required by JPA
    }

    public Product(String reference, String name, BigDecimal price) {
        this.reference = reference;
        this.name = name;
        this.price = price;
        this.active = true;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isActive() {
        return active;
    }
}
