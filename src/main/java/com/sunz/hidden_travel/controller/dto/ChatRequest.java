package com.sunz.hidden_travel.controller.dto;

import java.util.List;

/**
 * 챗봇 요청. history 는 이전 대화(최근 몇 턴)로, 화면이 들고 있다가 함께 보낸다.
 * (서버가 대화를 저장하지 않아 새로고침하면 초기화된다 — 지금 단계에서는 충분)
 */
public record ChatRequest(
        String message,
        List<ChatTurn> history
) {
    public record ChatTurn(String role, String text) {}
}
