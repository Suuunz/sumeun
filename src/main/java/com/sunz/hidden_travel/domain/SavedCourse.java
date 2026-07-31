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

    /* =========================================================
       실제 도로 경로 (카카오모빌리티 길찾기)
       코스당 한 번만 계산해 저장한다 — 경유지가 바뀌지 않는 한 다시 부를 이유가 없다.
       ========================================================= */

    /** 총 이동 거리(m). 아직 계산 전이면 null */
    @Column(name = "route_distance_m")
    private Integer routeDistanceMeters;

    /** 총 소요 시간(초). 아직 계산 전이면 null */
    @Column(name = "route_duration_s")
    private Integer routeDurationSeconds;

    /** 도로를 따라가는 좌표열 "lng,lat lng,lat ..." (JSON 보다 짧아 그대로 저장) */
    @Column(name = "route_path", columnDefinition = "TEXT")
    private String routePath;

    /** 경유지 수 */
    public int stopCount() {
        return stops.size();
    }

    public boolean hasRoute() {
        return routePath != null && !routePath.isBlank();
    }

    /** "1시간 20분" 형태 — 계산 전이면 null */
    public String durationText() {
        if (routeDurationSeconds == null || routeDurationSeconds <= 0) {
            return null;
        }
        int minutes = routeDurationSeconds / 60;
        int hours = minutes / 60;
        int rest = minutes % 60;
        if (hours > 0) {
            return rest > 0 ? hours + "시간 " + rest + "분" : hours + "시간";
        }
        return minutes + "분";
    }

    /** "12.5km" 형태 — 계산 전이면 null */
    public String distanceText() {
        if (routeDistanceMeters == null || routeDistanceMeters <= 0) {
            return null;
        }
        return routeDistanceMeters < 1000
                ? routeDistanceMeters + "m"
                : String.format("%.1fkm", routeDistanceMeters / 1000.0);
    }
}
