package com.fashionai.captioning.fashion_captioner.repository.mysql;

import com.fashionai.captioning.fashion_captioner.model.mysql.LlmResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LlmResponseRepository extends JpaRepository<LlmResponse, Long> {
} 