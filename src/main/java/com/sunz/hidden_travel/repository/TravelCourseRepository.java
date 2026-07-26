package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.TravelCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, Long> {

    List<TravelCourse> findBySigCd(String sigCd);

    boolean existsBySourceContentId(String sourceContentId);
}
