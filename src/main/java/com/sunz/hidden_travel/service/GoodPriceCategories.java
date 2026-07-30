package com.sunz.hidden_travel.service;

import java.util.List;

/**
 * 착한가격업소 업종 분류.
 *
 * 착한가격업소에는 식당뿐 아니라 미용실·세탁소·목욕탕 등 서비스 업종도 섞여 있다.
 * 먹거리 자리에 "커트 4,000원 · ○○미용실"이 뜨지 않도록 걸러낸다.
 *
 * 여러 곳에서 같은 기준을 써야 해서 한곳에 모은다.
 */
public final class GoodPriceCategories {

    /** 비식당(서비스) 업종을 가리키는 키워드 */
    private static final List<String> NON_FOOD = List.of(
            "비요식", "미용", "이용", "이미용", "세탁", "목욕", "숙박", "여관", "안경", "사진",
            "인쇄", "노래", "학원", "자동차", "수리", "헤어", "네일", "피부", "화장", "서비스"
    );

    private GoodPriceCategories() {
    }

    /** 식당으로 볼 수 있는 업종인지. 분류가 비어 있으면 식당으로 간주한다. */
    public static boolean isFood(String category) {
        if (category == null) {
            return true;
        }
        return NON_FOOD.stream().noneMatch(category::contains);
    }
}
