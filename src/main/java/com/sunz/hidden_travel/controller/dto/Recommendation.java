package com.sunz.hidden_travel.controller.dto;

/**
 * "다음엔 어디로?" 추천 카드용 더미 DTO.
 */
public record Recommendation(
        String region,
        String title,
        String desc
) {}
