package com.fashionai.captioning.fashion_captioner.model.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "llm_response")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Integer responseId;

    @Column(name = "user_query", nullable = false)
    private String userQuery;

    @Column(name = "response", nullable = false, columnDefinition = "LONGTEXT")
    private String response;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public LlmResponse(String userQuery, String response) {
        this.userQuery = userQuery;
        this.response = response;
        this.createdAt = LocalDateTime.now();
    }
} 