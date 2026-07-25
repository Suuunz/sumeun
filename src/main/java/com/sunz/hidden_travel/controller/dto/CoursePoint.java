package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 내 개별 경유지(타임라인) 더미 DTO.
 */
public record CoursePoint(
        int order,
        String name,
        String type,
        String desc,
        String arriveTime
) {}
