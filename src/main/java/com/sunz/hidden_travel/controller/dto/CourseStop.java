package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 편집기 타임라인의 경유지 더미 DTO.
 * CoursePoint 에 없는 편집기 전용 정보(착한가격 여부, 다음 지점까지의 이동)를 담는다.
 *
 * @param travelToNext 다음 지점까지의 이동 설명(마지막 지점은 null)
 * @param travelMode   이동수단 Material Symbols 아이콘명 (directions_car / directions_walk)
 */
public record CourseStop(
        int order,
        String name,
        String type,
        String arriveTime,
        boolean goodPrice,
        String travelToNext,
        String travelMode
) {}
