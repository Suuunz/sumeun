package com.sunz.hidden_travel.controller.dto;

import com.sunz.hidden_travel.mbti.TravelMbtiType;

/**
 * 후기 피드(/reviews)의 카드 1건.
 * coverPhoto 가 null 이면 사진 없는 후기 → 텍스트 카드로 렌더한다.
 *
 * mbti 는 작성자의 여행 MBTI (검사 전이면 null).
 * "어떤 성향의 사람이 이 코스를 어떻게 느꼈는지"가 피드의 핵심 정보라 함께 싣는다.
 */
public record ReviewCard(
        Long reviewId,
        String nickname,
        TravelMbtiType mbti,
        String sigCd,
        String regionLabel,
        String courseTitle,
        String coverPhoto,
        int photoCount,
        String excerpt,
        String createdAt
) {}
