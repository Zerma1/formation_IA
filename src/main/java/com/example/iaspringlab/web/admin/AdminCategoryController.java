package com.example.iaspringlab.web.admin;

import com.example.iaspringlab.category.CategoryService;
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
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        return showList(model, new CategoryForm());
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showList(model, form);
        }
        categoryService.create(form.toRequest());
        redirectAttributes.addFlashAttribute("success", "Catégorie créée.");
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", CategoryForm.from(categoryService.getById(id)));
        model.addAttribute("categoryId", id);
        return "admin/category-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CategoryForm form,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            return "admin/category-form";
        }
        categoryService.update(id, form.toRequest());
        redirectAttributes.addFlashAttribute("success", "Catégorie modifiée.");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        categoryService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Catégorie supprimée (les produits associés ont été conservés).");
        return "redirect:/admin/categories";
    }

    private String showList(Model model, CategoryForm form) {
        model.addAttribute("form", form);
        model.addAttribute("categories", categoryService.getAll());
        return "admin/categories";
    }
}
