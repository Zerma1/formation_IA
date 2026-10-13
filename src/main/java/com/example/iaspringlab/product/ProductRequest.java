package com.example.iaspringlab.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param categoryIds categories of the product; {@code null} means "not provided"
 *                    (no category on creation, unchanged on update), an empty list removes all categories.
 */
public record ProductRequest(

        @NotBlank(message = "reference is required")
        String reference,

        @NotBlank(message = "name is required")
        String name,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "price must be strictly positive")
        @Digits(integer = 17, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "active is required")
        Boolean active,

        List<@NotNull(message = "categoryIds must not contain null") Long> categoryIds
) {

    public ProductRequest(String reference, String name, BigDecimal price, Boolean active) {
        this(reference, name, price, active, null);
    }
}
