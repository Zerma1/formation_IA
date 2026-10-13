package com.example.iaspringlab.web.admin;

import com.example.iaspringlab.category.CategoryService;
import com.example.iaspringlab.product.ProductService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private static final String FORM_VIEW = "admin/product-form";

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.getAll());
        return "admin/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        return showForm(model, new ProductForm(), null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProductForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(model, form, null);
        }
        productService.create(form.toRequest());
        redirectAttributes.addFlashAttribute("success", "Produit créé.");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(model, ProductForm.from(productService.getById(id)), id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") ProductForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(model, form, id);
        }
        productService.update(id, form.toRequest());
        redirectAttributes.addFlashAttribute("success", "Produit modifié.");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Produit supprimé.");
        return "redirect:/admin/products";
    }

    private String showForm(Model model, ProductForm form, Long productId) {
        model.addAttribute("form", form);
        model.addAttribute("productId", productId);
        model.addAttribute("categories", categoryService.getAll());
        return FORM_VIEW;
    }
}
