package com.sunz.hidden_travel.controller.dto;

/**
 * 지도 검색 자동완성 / 빠른 선택용 지역 옵션 (코드 + 이름).
 */
public record RegionOption(
        String sigCd,
        String name
) {}
