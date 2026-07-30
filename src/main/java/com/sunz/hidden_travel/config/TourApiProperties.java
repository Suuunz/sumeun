package com.sunz.hidden_travel.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * TourAPI 설정 (application.yaml: tour.api.*)
 */
@Component
@ConfigurationProperties(prefix = "tour.api")
@Getter
@Setter
public class TourApiProperties {

    /** 공공데이터포털 인증키 */
    private String key;

    /** KorService2 엔드포인트 */
    private String endpoint = "https://apis.data.go.kr/B551011/KorService2";

    /**
     * 1일 호출 한도. 이 수를 넘기면 클라이언트가 호출을 중단한다.
     * 한도를 넘겨 배치가 중간에 실패하면 그날 남은 할당량까지 버리게 되므로,
     * 실제 한도(1000)보다 약간 낮게 잡아 여유를 둔다.
     */
    private int dailyCallLimit = 950;
}
