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
 * 착한가격업소 (행정안전부).
 */
@Entity
@Table(name = "good_price_shop", indexes = @Index(name = "idx_gps_sig", columnList = "sig_cd"))
@Getter
@Setter
@NoArgsConstructor
public class GoodPriceShop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sig_cd", length = 5, nullable = false)
    private String sigCd;

    private String name;

    private String category;

    private String menu;

    private int price;

    private String addr;

    private Double lat;

    private Double lng;

    private String source = "행정안전부";
}
