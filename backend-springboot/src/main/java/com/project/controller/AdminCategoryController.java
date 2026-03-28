package com.project.controller;

import com.project.model.Category;
import com.project.service.CategoryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/admin/categories")
    public String list(Model model,
                       @RequestParam(name = "error", required = false) String error) {
        model.addAttribute("categories", categoryService.listAll());
        model.addAttribute("error", error);
        return "admin/categories/list";
    }

    @GetMapping("/admin/categories/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CategoryForm());
        return "admin/categories/form";
    }

    @PostMapping("/admin/categories")
    public String create(@ModelAttribute("form") CategoryForm form,
                         BindingResult bindingResult) {
        if (form.getName() == null || form.getName().isBlank()) {
            bindingResult.rejectValue("name", "required", "Required");
        }
        if (bindingResult.hasErrors()) {
            return "admin/categories/form";
        }

        categoryService.create(form.getName(), form.getDescription());
        return "redirect:/admin/categories";
    }

    @GetMapping("/admin/categories/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Category category = categoryService.getById(id);
        model.addAttribute("form", CategoryForm.from(category));
        model.addAttribute("categoryId", id);
        return "admin/categories/form";
    }

    @PostMapping("/admin/categories/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") CategoryForm form,
                         BindingResult bindingResult,
                         Model model) {
        if (form.getName() == null || form.getName().isBlank()) {
            bindingResult.rejectValue("name", "required", "Required");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryId", id);
            return "admin/categories/form";
        }

        categoryService.update(id, form.getName(), form.getDescription());
        return "redirect:/admin/categories";
    }

    @PostMapping("/admin/categories/{id}/delete")
    public String delete(@PathVariable Long id) {
        try {
            categoryService.delete(id);
        } catch (IllegalStateException e) {
            if ("CATEGORY_IN_USE".equals(e.getMessage())) {
                return "redirect:/admin/categories?error=in_use";
            }
            throw e;
        }
        return "redirect:/admin/categories";
    }

    public static class CategoryForm {
        @NotBlank
        private String name;

        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public static CategoryForm from(Category category) {
            CategoryForm form = new CategoryForm();
            form.setName(category.getName());
            form.setDescription(category.getDescription());
            return form;
        }
    }
}
