package com.sunz.hidden_travel.domain;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 착한가격업소의 메뉴/가격 한 쌍 (GoodPriceShop 소속 값 객체).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class GoodPriceMenu {

    private String menu;

    /** 가격 (숫자 파싱 실패 시 null) */
    private Integer price;

    public GoodPriceMenu(String menu, Integer price) {
        this.menu = menu;
        this.price = price;
    }
}
