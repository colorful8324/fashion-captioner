package com.fashionai.captioning.fashion_captioner.service;

import com.fashionai.captioning.fashion_captioner.model.h2.Product;
import com.fashionai.captioning.fashion_captioner.repository.h2.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public List<Product> findAll() { return productRepository.findAll(); }
    public Optional<Product> findById(Long id) { return productRepository.findById(id); }
    public Product save(Product product) { return productRepository.save(product); }
    public void delete(Long id) { productRepository.deleteById(id); }
}
