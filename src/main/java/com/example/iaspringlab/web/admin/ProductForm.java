package com.example.iaspringlab.web.admin;

import com.example.iaspringlab.category.CategoryResponse;
import com.example.iaspringlab.product.ProductRequest;
import com.example.iaspringlab.product.ProductResponse;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutable backing object of the product HTML form.
 */
public class ProductForm {

    @NotBlank(message = "La référence est obligatoire.")
    private String reference;

    @NotBlank(message = "Le nom est obligatoire.")
    private String name;

    @NotNull(message = "Le prix est obligatoire.")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être strictement positif.")
    @Digits(integer = 17, fraction = 2, message = "Le prix doit avoir au plus 2 décimales.")
    private BigDecimal price;

    private boolean active = true;

    private List<Long> categoryIds = new ArrayList<>();

    public static ProductForm from(ProductResponse product) {
        ProductForm form = new ProductForm();
        form.reference = product.reference();
        form.name = product.name();
        form.price = product.price();
        form.active = product.active();
        form.categoryIds = new ArrayList<>(product.categories().stream().map(CategoryResponse::id).toList());
        return form;
    }

    public ProductRequest toRequest() {
        return new ProductRequest(reference, name, price, active, categoryIds == null ? List.of() : List.copyOf(categoryIds));
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Long> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Long> categoryIds) {
        this.categoryIds = categoryIds;
    }
}
