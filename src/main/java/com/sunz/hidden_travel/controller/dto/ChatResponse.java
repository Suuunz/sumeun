package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 챗봇 응답.
 *
 * @param answer          사용자에게 보여줄 답변
 * @param recommendations 이동 버튼으로 렌더할 추천 (DB 에 실재하는 것만 담긴다)
 * @param error           문제가 있으면 화면에 띄울 안내 (정상이면 null)
 */
public record ChatResponse(
        String answer,
        List<ChatRecommendation> recommendations,
        String error
) {
    public static ChatResponse of(String answer, List<ChatRecommendation> recommendations) {
        return new ChatResponse(answer, recommendations, null);
    }

    public static ChatResponse error(String message) {
        return new ChatResponse(null, List.of(), message);
    }
}
