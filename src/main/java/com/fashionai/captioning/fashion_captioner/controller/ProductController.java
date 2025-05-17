package com.fashionai.captioning.fashion_captioner.controller;

import com.fashionai.captioning.fashion_captioner.model.h2.Category;
import com.fashionai.captioning.fashion_captioner.model.h2.Product;
import com.fashionai.captioning.fashion_captioner.service.CategoryService;
import com.fashionai.captioning.fashion_captioner.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Controller
@RequestMapping("/admin/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        model.addAttribute("categories", categoryService.findAll());
        return "admin/product";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("isEdit", false);
        return "admin/product-form :: productFormModal";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Product product) {
        product.setDate(new Date());
        Optional<Category> category = categoryService.findById(product.getCategoryId());
        category.ifPresent(cat -> product.setCategoryName(cat.getName()));
        productService.save(product);
        return "redirect:/admin/product";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Optional<Product> product = productService.findById(id);
        if (product.isPresent()) {
            model.addAttribute("product", product.get());
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("isEdit", true);
            return "admin/product-form :: productFormModal";
        }
        return "redirect:/admin/product";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Product product) {
        product.setId(id);
        Optional<Category> category = categoryService.findById(product.getCategoryId());
        category.ifPresent(cat -> product.setCategoryName(cat.getName()));
        productService.save(product);
        return "redirect:/admin/product";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/product";
    }
}