package com.sunz.hidden_travel.controller.dto;

/**
 * 챗봇이 제시한 추천 1건. 화면에서 이동 버튼으로 렌더한다.
 *
 * @param type   region | course
 * @param title  버튼에 표시할 이름 (지역명 또는 코스명)
 * @param subtitle 보조 설명 (시도명, 추천 이유 등)
 * @param url    이동 경로 — 서버가 만들어 준다(모델이 만든 링크는 쓰지 않는다)
 */
public record ChatRecommendation(
        String type,
        String title,
        String subtitle,
        String url
) {}
