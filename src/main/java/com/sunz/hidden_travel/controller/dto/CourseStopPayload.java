package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 저장 요청의 경유지 1건. course.js 가 itemsJson 으로 보내는 JSON 배열 원소.
 * (순서는 배열 순서를 그대로 따른다)
 */
public record CourseStopPayload(
        String name,
        String type,
        boolean sage
) {}
