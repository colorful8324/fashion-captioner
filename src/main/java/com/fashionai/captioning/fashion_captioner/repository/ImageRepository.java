package com.fashionai.captioning.fashion_captioner.repository;

import com.fashionai.captioning.fashion_captioner.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Integer> {
}