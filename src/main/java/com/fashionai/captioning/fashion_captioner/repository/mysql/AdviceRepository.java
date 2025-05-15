package com.fashionai.captioning.fashion_captioner.repository.mysql;

import com.fashionai.captioning.fashion_captioner.model.mysql.Advice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdviceRepository extends JpaRepository<Advice, Integer> {
}
