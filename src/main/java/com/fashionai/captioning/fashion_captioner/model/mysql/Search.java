package com.fashionai.captioning.fashion_captioner.model.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "search_history")
public class Search {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_id")
    private Integer searchId;
    @Column(name = "record_id")
    private Integer recordId;
    @Column(name = "search_query")
    private String searchQuery;

    public Search(Integer recordId, String searchQuery) {
        this.recordId = recordId;
        this.searchQuery = searchQuery;
    }
}
