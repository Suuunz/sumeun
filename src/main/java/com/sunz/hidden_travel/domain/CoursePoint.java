package com.sunz.hidden_travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 추천 코스의 개별 경유지 (TravelCourse 에 소속된 값 객체).
 * order 는 예약어라 컬럼명을 point_order 로 매핑한다.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class CoursePoint {

    @Column(name = "point_order")
    private int order;

    private String name;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String description;
}
