package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 후기 상세(/review/{id}) 화면 모델. 공유 링크가 가리키는 대상.
 * mine 은 현재 사용자가 작성자인지 여부(수정 버튼 노출 판단).
 */
public record ReviewDetail(
        Long reviewId,
        Long courseId,
        String nickname,
        com.sunz.hidden_travel.mbti.TravelMbtiType mbti,
        String sigCd,
        String regionLabel,
        String courseTitle,
        List<String> stopNames,
        List<String> photos,
        String content,
        String createdAt,
        boolean shared,
        boolean mine
) {}
