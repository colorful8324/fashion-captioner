package com.fashionai.captioning.fashion_captioner.repository.h2;

import com.fashionai.captioning.fashion_captioner.model.h2.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}