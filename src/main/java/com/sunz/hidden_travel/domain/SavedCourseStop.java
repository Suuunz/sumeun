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

    public SavedCourseStop(int order, String name, String type, boolean sage) {
        this.order = order;
        this.name = name;
        this.type = type;
        this.sage = sage;
    }
}
