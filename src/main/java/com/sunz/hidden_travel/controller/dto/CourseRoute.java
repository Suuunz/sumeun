package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 코스의 실제 도로 경로.
 *
 * @param distanceText 총 이동 거리 "12.5km" (계산 전이면 null)
 * @param durationText 총 소요 시간 "1시간 20분" (계산 전이면 null)
 * @param path         도로를 따라가는 좌표열 [[lng, lat], ...]
 * @param available    경로를 구했는지 — false 면 화면은 직선 동선을 유지한다
 * @param message      구하지 못한 이유 (available=true 면 null)
 */
public record CourseRoute(
        String distanceText,
        String durationText,
        List<double[]> path,
        boolean available,
        String message
) {}
