package com.fashionai.captioning.fashion_captioner.config;

import com.fashionai.captioning.fashion_captioner.model.Role;
import com.fashionai.captioning.fashion_captioner.model.Status;
import com.fashionai.captioning.fashion_captioner.model.h2.User;
import com.fashionai.captioning.fashion_captioner.repository.h2.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            userRepository.save(new User(null, "Nguyễn Văn A", "a.nguyen@example.com", Role.ADMIN, Status.ACTIVE, LocalDateTime.now()));
            userRepository.save(new User(null, "Trần Thị B", "b.tran@example.com", Role.USER, Status.SUSPENDED, LocalDateTime.now()));
            userRepository.save(new User(null, "Lê Văn C", "c.le@example.com", Role.USER, Status.ACTIVE, LocalDateTime.now()));
            userRepository.save(new User(null, "Phạm Thị D", "d.pham@example.com", Role.USER, Status.ACTIVE, LocalDateTime.now()));
            userRepository.save(new User(null, "Đỗ Văn E", "e.do@example.com", Role.USER, Status.SUSPENDED, LocalDateTime.now()));
        };
    }
}
