package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.FoodPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FoodPlaceRepository extends JpaRepository<FoodPlace, Long> {

    List<FoodPlace> findBySigCd(String sigCd);

    boolean existsBySourceContentId(String sourceContentId);
}
