package com.fashionai.captioning.fashion_captioner.config;

import com.fashionai.captioning.fashion_captioner.model.CategoryStatus;
import com.fashionai.captioning.fashion_captioner.model.ProductStatus;
import com.fashionai.captioning.fashion_captioner.model.Role;
import com.fashionai.captioning.fashion_captioner.model.UserStatus;
import com.fashionai.captioning.fashion_captioner.model.h2.Category;
import com.fashionai.captioning.fashion_captioner.model.h2.Product;
import com.fashionai.captioning.fashion_captioner.model.h2.User;
import com.fashionai.captioning.fashion_captioner.repository.h2.CategoryRepository;
import com.fashionai.captioning.fashion_captioner.repository.h2.ProductRepository;
import com.fashionai.captioning.fashion_captioner.repository.h2.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            userRepository.save(new User(null, "Nguyễn Văn A", "a.nguyen@example.com", Role.ADMIN, UserStatus.ACTIVE, new Date()));
            userRepository.save(new User(null, "Trần Thị B", "b.tran@example.com", Role.USER, UserStatus.SUSPENDED, new Date()));
            userRepository.save(new User(null, "Lê Văn C", "c.le@example.com", Role.USER, UserStatus.ACTIVE, new Date()));
            userRepository.save(new User(null, "Phạm Thị D", "d.pham@example.com", Role.USER, UserStatus.ACTIVE, new Date()));
            userRepository.save(new User(null, "Đỗ Văn E", "e.do@example.com", Role.USER, UserStatus.SUSPENDED, new Date()));
        };
    }

    @Bean
    CommandLineRunner initCategory(CategoryRepository categoryRepository) {
        return args -> {
            Category cat1 = new Category(null, "Quần áo nam", "Danh mục quần áo cho nam giới", CategoryStatus.DISPLAY, new Date());
            Category cat2 = new Category(null, "Phụ kiện", "Danh mục phụ kiện thời trang", CategoryStatus.HIDDEN, new Date());
            categoryRepository.saveAll(List.of(cat1, cat2));
        };
    }

    @Bean
    CommandLineRunner initProduct(ProductRepository productRepository) {
        return args -> {
            productRepository.saveAll(List.of(
                new Product(null, "Áo thun nam", "Quần áo nam", (long) 1.0, 15.0, ProductStatus.AVAILABLE, new Date()),
                new Product(null, "Ví da", "Phụ kiện", (long) 2.0, 16.5, ProductStatus.UNAVAILABLE, new Date())
            ));
        };
    }
}
