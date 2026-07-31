package com.sunz.hidden_travel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 카카오모빌리티 길찾기(Directions) 설정 — 코스의 실제 도로 경로·소요시간·거리에 쓴다.
 *
 * 지도 표시에 쓰는 JavaScript 키와 <b>다른 키</b>가 필요하다.
 * 같은 카카오 앱의 <b>REST API 키</b>를 쓰며, 헤더에 "KakaoAK {키}" 로 넣는다.
 *
 * ./config/application-secret.yaml 에 아래처럼 넣는다:
 *
 *   kakao:
 *     mobility:
 *       rest-api-key: 발급받은_REST_API_키
 */
@Component
@ConfigurationProperties(prefix = "kakao.mobility")
@Getter
@Setter
public class KakaoMobilityProperties {

    /** 카카오 REST API 키 (JavaScript 키가 아니다) */
    private String restApiKey;

    /** 다중 경유지 길찾기 엔드포인트 */
    private String endpoint = "https://apis-navi.kakaomobility.com/v1/waypoints/directions";

    /**
     * 1일 호출 한도. 경로는 코스당 한 번만 계산해 DB 에 저장하므로 호출이 많지 않지만,
     * 실수로 반복 호출하는 상황을 막는 안전장치.
     */
    private int dailyCallLimit = 300;

    private int timeoutSeconds = 15;

    public boolean isConfigured() {
        return restApiKey != null && !restApiKey.isBlank();
    }
}
