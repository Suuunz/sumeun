package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 만들기 오른쪽 타임라인 초기 항목(추천 코스에서 담아온 경유지).
 */
public record CourseInitItem(
        int order,
        String name,
        String type,
        boolean sage
) {}
