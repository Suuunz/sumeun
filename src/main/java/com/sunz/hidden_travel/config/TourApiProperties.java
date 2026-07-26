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
}
