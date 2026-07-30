package com.sunz.hidden_travel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini 설정 (application.yaml: gemini.api.*)
 *
 * 인증키는 커밋하지 않는다. ./config/application-secret.yaml 에 아래처럼 넣는다:
 *
 *   gemini:
 *     api:
 *       key: 여기에_발급받은_키
 *
 * (환경변수 GEMINI_API_KEY 로도 주입된다 — relaxed binding)
 */
@Component
@ConfigurationProperties(prefix = "gemini.api")
@Getter
@Setter
public class GeminiProperties {

    /** Google AI Studio 에서 발급받은 API 키 */
    private String key;

    /**
     * 사용할 모델. 무료 등급에서 쓸 수 있는 모델로 기본값을 둔다.
     * 사용 가능한 모델이 바뀌면 이 값만 설정으로 바꾸면 된다.
     */
    private String model = "gemini-2.0-flash";

    private String endpoint = "https://generativelanguage.googleapis.com/v1beta";

    /** 응답 대기 한도(초) — 무료 등급은 지연이 있을 수 있다 */
    private int timeoutSeconds = 30;

    /** 키가 설정되어 있는지 */
    public boolean isConfigured() {
        return key != null && !key.isBlank();
    }
}
