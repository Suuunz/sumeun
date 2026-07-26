package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.RegionSummary;
import com.sunz.hidden_travel.domain.Specialty;
import com.sunz.hidden_travel.repository.SpecialtyRepository;
import com.sunz.hidden_travel.service.DummyRegionData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 지역 정보 JSON API.
 * 지도에서 시군구를 클릭하면 SIG_CD 로 이 엔드포인트를 호출해 패널을 채운다.
 *
 * 특산물(specialties)은 DB(Specialty)에서 조회해 채운다. (적재되지 않은 지역은 더미 유지)
 * 나머지 필드는 아직 DummyRegionData 를 쓰며, 이후 단계에서 순차적으로 DB로 전환한다.
 */
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final DummyRegionData data;
    private final SpecialtyRepository specialtyRepository;

    public RegionController(DummyRegionData data, SpecialtyRepository specialtyRepository) {
        this.data = data;
        this.specialtyRepository = specialtyRepository;
    }

    /** GET /api/regions/{sigCd} → 해당 지역 RegionSummary (JSON) */
    @GetMapping("/{sigCd}")
    public RegionSummary region(@PathVariable String sigCd) {
        RegionSummary base = data.get(sigCd);

        // 특산물은 DB 우선
        List<String> dbSpecialties = specialtyRepository.findBySigCd(sigCd).stream()
                .map(Specialty::getName)
                .toList();
        if (!dbSpecialties.isEmpty()) {
            base = new RegionSummary(base.name(), base.province(), base.aiSummary(),
                    dbSpecialties, base.shops(), base.briefCourse());
        }
        return base;
    }
}
