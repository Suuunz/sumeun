package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 만들기 왼쪽 후보 카드 1건.
 * type: attraction / food / goodprice / specialty
 * sage: 착한가격업소 여부(배지 표시), priceText: 착한가격업소 메뉴·가격(그 외 null)
 */
public record CandidateItem(
        String id,
        String type,
        String name,
        String desc,
        String category,
        boolean sage,
        String priceText
) {}
