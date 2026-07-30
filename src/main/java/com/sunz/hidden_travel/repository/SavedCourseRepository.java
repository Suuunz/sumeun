package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    /** 내 코스 목록 — 최근 저장 순 */
    List<SavedCourse> findByUserIdOrderByCreatedAtDesc(Long userId);
}
