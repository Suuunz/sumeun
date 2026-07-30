package com.sunz.hidden_travel.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 사용자가 직접 만들어 저장한 코스("내 코스").
 * TourAPI 에서 받아온 추천 코스({@link TravelCourse})와 달리 사용자 소유이고 후기의 대상이 된다.
 */
@Entity
@Table(name = "saved_course", indexes = {
        @Index(name = "idx_saved_course_user", columnList = "user_id"),
        @Index(name = "idx_saved_course_sig", columnList = "sig_cd")
})
@Getter
@Setter
@NoArgsConstructor
public class SavedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 작성자 {@link AppUser#getId()} (연관관계 대신 식별자만 보관 — 조회가 단순하고 인증 교체에 유리) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sig_cd", length = 5, nullable = false)
    private String sigCd;

    @Column(nullable = false)
    private String title;

    /** 담긴 착한가격업소 수 (저장 시점 집계값) */
    @Column(name = "good_price_count")
    private int goodPriceCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ElementCollection
    @CollectionTable(name = "saved_course_stop", joinColumns = @JoinColumn(name = "saved_course_id"))
    @OrderColumn(name = "stop_index")
    private List<SavedCourseStop> stops = new ArrayList<>();

    /** 경유지 수 */
    public int stopCount() {
        return stops.size();
    }
}
