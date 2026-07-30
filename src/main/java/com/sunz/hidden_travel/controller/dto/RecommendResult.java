package com.sunz.hidden_travel.controller.dto;

/**
 * AI 추천 결과. 프론트가 지도 선택(selectRegion)에 사용한다.
 * 추천 실패 시 sigCd/name 은 null.
 */
public record RecommendResult(
        String sigCd,
        String name
) {}
