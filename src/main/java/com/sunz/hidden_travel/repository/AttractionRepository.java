package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    List<Attraction> findBySigCd(String sigCd);

    boolean existsBySourceContentId(String sourceContentId);

    /** 관광지가 적재된 시군구 코드 목록(추천 후보 = 실제 보여줄 데이터가 있는 지역) */
    @Query("select distinct a.sigCd from Attraction a")
    List<String> findDistinctSigCd();
}
