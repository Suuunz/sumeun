package com.sunz.hidden_travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시군구 지역. SIG_CD(5자리 행정표준 시군구 코드)를 PK로 사용한다.
 * 모든 하위 엔티티는 sigCd 로 이 지역과 연관된다.
 */
@Entity
@Table(name = "region")
@Getter
@Setter
@NoArgsConstructor
public class Region {

    @Id
    @Column(name = "sig_cd", length = 5)
    private String sigCd;

    private String name;

    private String province;

    private Double lat;

    private Double lng;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    /** 저평가 지수 (5단계에서 채움) */
    private Integer underratedScore;
}
