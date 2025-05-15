package com.fashionai.captioning.fashion_captioner.repository.mysql;

import com.fashionai.captioning.fashion_captioner.model.mysql.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Integer> {
}