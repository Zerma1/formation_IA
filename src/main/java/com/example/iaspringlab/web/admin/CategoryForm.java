package com.example.iaspringlab.web.admin;

import com.example.iaspringlab.category.CategoryRequest;
import com.example.iaspringlab.category.CategoryResponse;
import jakarta.validation.constraints.NotBlank;

/**
 * Mutable backing object of the category HTML form.
 */
public class CategoryForm {

    @NotBlank(message = "Le nom est obligatoire.")
    private String name;

    public static CategoryForm from(CategoryResponse category) {
        CategoryForm form = new CategoryForm();
        form.name = category.name();
        return form;
    }

    public CategoryRequest toRequest() {
        return new CategoryRequest(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
