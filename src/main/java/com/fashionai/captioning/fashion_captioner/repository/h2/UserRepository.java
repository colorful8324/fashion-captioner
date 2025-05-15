package com.fashionai.captioning.fashion_captioner.repository.h2;

import com.fashionai.captioning.fashion_captioner.model.h2.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface UserRepository extends JpaRepository<User, Long> {
}