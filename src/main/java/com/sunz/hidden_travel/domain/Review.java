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
 * 저장된 코스({@link SavedCourse})에 대한 여행 후기.
 * 사진과 줄글로 구성되고, 공개(shared)로 두면 후기 피드에 노출된다.
 */
@Entity
@Table(name = "review", indexes = {
        @Index(name = "idx_review_course", columnList = "saved_course_id"),
        @Index(name = "idx_review_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 후기 대상 코스 {@link SavedCourse#getId()} */
    @Column(name = "saved_course_id", nullable = false)
    private Long savedCourseId;

    /** 작성자 {@link AppUser#getId()} */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 코스가 속한 지역 (피드에서 지역별로 보여주기 위해 비정규화 보관) */
    @Column(name = "sig_cd", length = 5, nullable = false)
    private String sigCd;

    /** 줄글 후기 */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 공개 여부 — true 면 후기 피드(/reviews)에 노출 */
    @Column(nullable = false)
    private boolean shared = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 사진 경로 목록 (예: /uploads/reviews/xxxx.jpg) */
    @ElementCollection
    @CollectionTable(name = "review_photo", joinColumns = @JoinColumn(name = "review_id"))
    @OrderColumn(name = "photo_index")
    @Column(name = "path")
    private List<String> photoPaths = new ArrayList<>();

    /** 대표 사진 (없으면 null) */
    public String coverPhoto() {
        return photoPaths.isEmpty() ? null : photoPaths.get(0);
    }
}
