package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, String> {

    /** sigCd 가 PK 이므로 findById 와 동일하나, API 일관성을 위해 제공 */
    Optional<Region> findBySigCd(String sigCd);

    /** 관광지가 하나도 적재되지 않은 시군구 코드 (적재 대상 확인용) */
    @Query("""
            select r.sigCd from Region r
            where r.sigCd not in (select distinct a.sigCd from Attraction a)
            order by r.sigCd
            """)
    List<String> findSigCdsWithoutAttraction();

    /** 관광지가 빈 시군구를 가진 시도 코드(SIG_CD 앞 2자리) — 빈 지역이 많은 시도 순 */
    @Query("""
            select substring(r.sigCd, 1, 2) from Region r
            where r.sigCd not in (select distinct a.sigCd from Attraction a)
            group by substring(r.sigCd, 1, 2)
            order by count(r) desc
            """)
    List<String> findSidoPrefixesWithoutAttraction();
}
