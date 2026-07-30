package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 지역 상세의 "이 지역은 이런 곳이에요" 블록.
 *
 * TourAPI 에는 <b>지역 단위 소개글이 없다</b>(areaCode2 는 코드·이름만 준다).
 * 그래서 적재된 실데이터로 각 블록을 구성한다.
 * 랜드마크 설명만 콘텐츠 단위 소개글(detailCommon2 의 overview)이 있어,
 * 화면에서 비동기로 채운다(호출 한도를 페이지 로딩에 묶지 않기 위해).
 *
 * 데이터가 없는 블록은 화면에서 통째로 감춘다 — 빈 제목만 남기지 않는다.
 */
public record RegionIntro(
        String overview,
        List<Landmark> landmarks,
        String foodSummary,
        List<String> goodPriceHighlights,
        List<String> specialties
) {
    /**
     * 대표 랜드마크. description 은 최초에 비어 있고
     * 화면이 /api/attraction/{id} 로 채운다(그 API 가 결과를 DB 에 캐시한다).
     */
    public record Landmark(Long id, String name, String addr, String image, String description) {}

    public boolean hasLandmarks() {
        return landmarks != null && !landmarks.isEmpty();
    }

    public boolean hasFood() {
        return (foodSummary != null && !foodSummary.isBlank())
                || (goodPriceHighlights != null && !goodPriceHighlights.isEmpty());
    }

    public boolean hasSpecialties() {
        return specialties != null && !specialties.isEmpty();
    }
}
