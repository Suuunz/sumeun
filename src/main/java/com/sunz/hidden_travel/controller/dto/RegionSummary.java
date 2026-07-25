package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 지역 상세(패널/전체 페이지) 화면용 더미 DTO.
 */
public record RegionSummary(
        String name,
        String province,
        String aiSummary,
        List<String> specialties,
        List<GoodPriceShop> shops,
        List<CoursePoint> briefCourse
) {}
