package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * AI 추천 요청. 지금(랜덤)은 사용하지 않지만 AI 교체를 위해 그대로 전달·보관한다.
 */
public record RecommendRequest(
        List<String> styles,
        String mood,
        String freeText
) {}
