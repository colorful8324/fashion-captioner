package com.fashionai.captioning.fashion_captioner.model.h2;

import com.fashionai.captioning.fashion_captioner.model.Role;
import com.fashionai.captioning.fashion_captioner.model.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private Role role;
    private Status status;
    private LocalDateTime createdAt;
}

