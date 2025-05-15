package com.fashionai.captioning.fashion_captioner.service;

import com.fashionai.captioning.fashion_captioner.model.h2.Category;
import com.fashionai.captioning.fashion_captioner.repository.h2.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findAll() { return categoryRepository.findAll(); }
    public Optional<Category> findById(Long id) { return categoryRepository.findById(id); }
    public Category save(Category category) { return categoryRepository.save(category); }
    public void delete(Long id) { categoryRepository.deleteById(id); }
}