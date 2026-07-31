package com.sunz.hidden_travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 외부 API 호출 사용량 (서비스별 · 날짜별).
 *
 * 메모리 카운터만 두면 앱을 재시작할 때마다 0 으로 돌아가 하루 한도를 넘길 수 있다.
 * (개발 중에는 재시작이 잦아 실제로 문제가 된다)
 *
 * id 는 "서비스:날짜" 형태 — 예) "tour:2026-07-31", "route:2026-07-31".
 * 날짜가 키에 들어 있어 자정이 지나면 자연히 새 행이 되고, 별도 리셋이 필요 없다.
 */
@Entity
@Table(name = "api_call_usage")
@Getter
@Setter
@NoArgsConstructor
public class ApiCallUsage {

    @Id
    @Column(name = "usage_key", length = 60)
    private String key;

    @Column(name = "call_count", nullable = false)
    private int count;

    public ApiCallUsage(String key, int count) {
        this.key = key;
        this.count = count;
    }

    public static String keyOf(String service, LocalDate date) {
        return service + ":" + date;
    }
}
