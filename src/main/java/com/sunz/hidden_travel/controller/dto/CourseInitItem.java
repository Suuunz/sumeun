package com.sunz.hidden_travel.controller.dto;

/**
 * 코스 만들기 오른쪽 타임라인 초기 항목(추천 코스에서 담아온 경유지).
 *
 * 경유지도 TourAPI 독립 콘텐츠라서 관광지와 동일하게 사진·상세를 제공한다.
 * - attractionId: 이미 적재된 관광지와 매칭되면 그 id (→ /api/attraction/{id})
 * - contentId   : 매칭되지 않았을 때 쓰는 TourAPI contentId
 *                 (→ /api/attraction/by-content/{contentId}, 조회 시 관광지로 저장)
 */
public record CourseInitItem(
        int order,
        String name,
        String type,
        boolean sage,
        Long attractionId,
        String contentId,
        String image,
        String addr
) {}
