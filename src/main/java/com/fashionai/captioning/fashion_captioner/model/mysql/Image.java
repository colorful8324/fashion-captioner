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
@Table(name = "image_caption")
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "caption_generated", columnDefinition = "TEXT")
    private String captionGenerated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Image(String imageName, String imageUrl, String captionGenerated) {
        this.imageName = imageName;
        this.imageUrl = imageUrl;
        this.captionGenerated = captionGenerated;
        this.createdAt = LocalDateTime.now();
    }

}
