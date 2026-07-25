package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, String> {

    /** sigCd 가 PK 이므로 findById 와 동일하나, API 일관성을 위해 제공 */
    Optional<Region> findBySigCd(String sigCd);
}
