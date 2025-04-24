package com.fashionai.captioning.fashion_captioner.repository;

import com.fashionai.captioning.fashion_captioner.model.Caption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptionRepository extends JpaRepository<Caption, Integer> {
}