package com.sunz.hidden_travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 음식·먹거리 장소.
 */
@Entity
@Table(name = "food_place", indexes = @Index(name = "idx_food_sig", columnList = "sig_cd"))
@Getter
@Setter
@NoArgsConstructor
public class FoodPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sig_cd", length = 5, nullable = false)
    private String sigCd;

    private String name;

    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String addr;

    private Double lat;

    private Double lng;

    /** TourAPI contentId (중복 적재 방지 키) */
    private String sourceContentId;
}
