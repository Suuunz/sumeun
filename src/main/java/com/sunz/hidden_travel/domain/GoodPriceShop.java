package com.sunz.hidden_travel.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * 착한가격업소 (행정안전부).
 * 대표 메뉴/가격은 menu/price 에, 나머지 메뉴들은 menus(값 컬렉션)에 보존한다.
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

    /** 대표 메뉴 */
    private String menu;

    /** 대표 가격 (파싱 실패 시 null) */
    private Integer price;

    private String phone;

    private String addr;

    private Double lat;

    private Double lng;

    private String source = "행정안전부";

    /** 전체 메뉴/가격 (대표 포함) */
    @ElementCollection
    @CollectionTable(name = "good_price_menu", joinColumns = @JoinColumn(name = "shop_id"))
    @OrderColumn(name = "menu_index")
    private List<GoodPriceMenu> menus = new ArrayList<>();
}
