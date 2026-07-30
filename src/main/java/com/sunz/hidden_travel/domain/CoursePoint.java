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

    /**
     * 경유지 자체의 TourAPI contentId(detailInfo2 의 subcontentid).
     * 경유지는 코스에 딸린 텍스트가 아니라 독립 콘텐츠라서, 이 값으로
     * 관광지와 완전히 동일하게 상세를 조회할 수 있다.
     */
    @Column(name = "content_id")
    private String contentId;

    /** 경유지 대표 이미지(subdetailimg) — detailInfo2 응답에 이미 포함되어 추가 호출이 없다 */
    @Column(columnDefinition = "TEXT")
    private String image;
}
