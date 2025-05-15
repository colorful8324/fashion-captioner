package com.fashionai.captioning.fashion_captioner.repository.mysql;

import com.fashionai.captioning.fashion_captioner.model.mysql.Search;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchRepository extends JpaRepository<Search, Integer> {
}
