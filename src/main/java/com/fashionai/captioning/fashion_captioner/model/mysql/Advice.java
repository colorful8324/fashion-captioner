package com.fashionai.captioning.fashion_captioner.model.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "llm_response")
public class Advice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Integer id;
    @Column(name = "user_query")
    private String query;
    @Column(name = "response")
    private String response;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Advice(String query, String response) {
        this.query = query;
        this.response = response;
        this.createdAt = LocalDateTime.now();
    }
}
