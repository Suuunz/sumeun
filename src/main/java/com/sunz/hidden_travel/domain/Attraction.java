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
 * 관광지 (TourAPI 콘텐츠).
 */
@Entity
@Table(name = "attraction", indexes = @Index(name = "idx_attraction_sig", columnList = "sig_cd"))
@Getter
@Setter
@NoArgsConstructor
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sig_cd", length = 5, nullable = false)
    private String sigCd;

    private String name;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String addr;

    private Double lat;

    private Double lng;

    /** TourAPI contentId */
    private String sourceContentId;

    private String image;
}
