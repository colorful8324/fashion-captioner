package com.fashionai.captioning.fashion_captioner.model.h2;

import com.fashionai.captioning.fashion_captioner.model.Role;
import com.fashionai.captioning.fashion_captioner.model.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;

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
    private UserStatus userStatus;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createdAt;
}
