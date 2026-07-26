package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 추천 코스 / 저장된 코스 카드용 DTO.
 * courseId 는 TravelCourse 식별자(추천 코스 → 내 코스 담기 이동에 사용, 없으면 null).
 */
public record CourseCard(
        String title,
        String theme,
        String duration,
        List<String> points,
        String distance,
        Long courseId
) {}
