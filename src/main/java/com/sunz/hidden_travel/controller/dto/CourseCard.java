package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 추천 코스 / 저장된 코스 카드용 더미 DTO.
 */
public record CourseCard(
        String title,
        String theme,
        String duration,
        List<String> points,
        String distance
) {}
