package com.fashionai.captioning.fashion_captioner.repository.h2;

import com.fashionai.captioning.fashion_captioner.model.h2.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}