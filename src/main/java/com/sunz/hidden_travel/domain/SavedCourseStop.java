package com.sunz.hidden_travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 저장된 코스의 개별 경유지 ({@link SavedCourse} 에 소속된 값 객체).
 * order 는 예약어라 컬럼명을 stop_order 로 매핑한다({@link CoursePoint} 와 동일 규칙).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class SavedCourseStop {

    @Column(name = "stop_order")
    private int order;

    private String name;

    /** attraction / food / goodprice / specialty */
    private String type;

    /** 착한가격업소 여부(배지 표시용) */
    private boolean sage;

    /**
     * 좌표 — 지도에 동선을 그리는 데 쓴다.
     * 원본(Attraction/FoodPlace/GoodPriceShop)에 이미 있는 값을 담을 때 함께 넘겨받는다.
     * 주소로 다시 지오코딩하면 경유지마다 호출이 들고 정확도도 떨어진다.
     * 좌표가 없는 항목(특산물 등)은 null 이고, 지도에서는 건너뛴다.
     */
    private Double lat;

    private Double lng;

    public SavedCourseStop(int order, String name, String type, boolean sage, Double lat, Double lng) {
        this.order = order;
        this.name = name;
        this.type = type;
        this.sage = sage;
        this.lat = lat;
        this.lng = lng;
    }

    /** 지도에 찍을 수 있는 경유지인지 */
    public boolean hasCoord() {
        return lat != null && lng != null;
    }
}
