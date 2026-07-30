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
 * 날짜별 외부 API 호출 사용량.
 *
 * 메모리 카운터만 두면 앱을 재시작할 때마다 0 으로 돌아가 하루 한도를 넘길 수 있다.
 * (개발 중에는 재시작이 잦아 실제로 문제가 된다)
 */
@Entity
@Table(name = "api_call_usage")
@Getter
@Setter
@NoArgsConstructor
public class ApiCallUsage {

    /** 사용일 (KST 기준) */
    @Id
    @Column(name = "usage_date")
    private LocalDate date;

    @Column(name = "call_count", nullable = false)
    private int count;

    public ApiCallUsage(LocalDate date, int count) {
        this.date = date;
        this.count = count;
    }
}
