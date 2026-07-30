package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** 후기 피드 — 공개된 후기만, 최신순 */
    List<Review> findBySharedTrueOrderByCreatedAtDesc();

    /** 코스 1건에 달린 후기(코스당 1개로 운영) */
    Optional<Review> findFirstBySavedCourseId(Long savedCourseId);

    /** 내가 쓴 후기 목록 */
    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId);
}
