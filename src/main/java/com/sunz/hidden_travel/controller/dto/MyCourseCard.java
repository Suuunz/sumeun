package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 내 코스 목록의 카드 1건.
 * reviewId 가 null 이면 아직 후기를 쓰지 않은 코스 → "후기 쓰기", 있으면 "후기 보기".
 */
public record MyCourseCard(
        Long courseId,
        String title,
        String sigCd,
        String regionLabel,
        int stopCount,
        int goodPriceCount,
        List<String> stopNames,
        String createdAt,
        Long reviewId
) {
    public boolean hasReview() {
        return reviewId != null;
    }
}
