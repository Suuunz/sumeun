package com.sunz.hidden_travel.controller.dto;

/**
 * 후기 피드(/reviews)의 카드 1건.
 * coverPhoto 가 null 이면 사진 없는 후기 → 텍스트 카드로 렌더한다.
 */
public record ReviewCard(
        Long reviewId,
        String nickname,
        String sigCd,
        String regionLabel,
        String courseTitle,
        String coverPhoto,
        int photoCount,
        String excerpt,
        String createdAt
) {}
