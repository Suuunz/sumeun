package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 저장 요청의 경유지 1건. course.js 가 itemsJson 으로 보내는 JSON 배열 원소.
 * (순서는 배열 순서를 그대로 따른다)
 *
 * lat/lng 는 지도에 동선을 그리기 위한 좌표 — 원본 데이터에 있는 값을 그대로 실어 보낸다.
 */
public record CourseStopPayload(
        String name,
        String type,
        boolean sage,
        Double lat,
        Double lng
) {}
