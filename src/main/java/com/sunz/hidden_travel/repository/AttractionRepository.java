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

    /** 시군구별 관광지 수 — 챗봇 카탈로그용 (row: [sigCd, count]) */
    @Query("select a.sigCd, count(a) from Attraction a group by a.sigCd")
    List<Object[]> countBySigCd();

    /**
     * '숨은 여행지' 후보 시군구 — 광역시·경기·제주를 뺀 지역 중
     * 보여줄 관광지가 일정 수 이상 쌓인 곳.
     * (인기도 지표가 없어 "덜 알려진 곳"을 이렇게 근사한다)
     */
    @Query("""
            select a.sigCd from Attraction a
            where substring(a.sigCd, 1, 2) not in :excludedSido
            group by a.sigCd
            having count(a) >= :minCount
            """)
    List<String> findHiddenCandidateSigCds(List<String> excludedSido, long minCount);

    /** 이미지가 있는 관광지 (스포트라이트 카드 사진용) */
    List<Attraction> findBySigCdAndImageIsNotNull(String sigCd);
}
