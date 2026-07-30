package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    List<Attraction> findBySigCd(String sigCd);

    boolean existsBySourceContentId(String sourceContentId);

    /** TourAPI contentId 로 조회 — 여행코스 경유지를 관광지와 연결할 때 쓴다 */
    Optional<Attraction> findFirstBySourceContentId(String sourceContentId);

    /** 관광지가 적재된 시군구 코드 목록(추천 후보 = 실제 보여줄 데이터가 있는 지역) */
    @Query("select distinct a.sigCd from Attraction a")
    List<String> findDistinctSigCd();
}
