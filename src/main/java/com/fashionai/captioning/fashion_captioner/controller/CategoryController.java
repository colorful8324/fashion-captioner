package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.h2.Category;
import com.fashionai.captioning.fashion_captioner.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

@Controller
@RequestMapping("/admin/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/category";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("isEdit", false);
        return "admin/category-form :: categoryFormModal";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Category category) {
        category.setCreatedDate(new Date());
        categoryService.save(category);
        return "redirect:/admin/category";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Optional<Category> category = categoryService.findById(id);
        if (category.isPresent()) {
            model.addAttribute("category", category.get());
            model.addAttribute("isEdit", true);
            return "admin/category-form :: categoryFormModal";
        }
        return "redirect:/admin/category";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Category category) {
        category.setId(id);
        categoryService.findById(id).ifPresent(existing -> category.setCreatedDate(existing.getCreatedDate()));
        categoryService.save(category);
        return "redirect:/admin/category";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        categoryService.delete(id);
        return "redirect:/admin/category";
    }
}
