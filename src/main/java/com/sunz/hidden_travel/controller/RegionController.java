package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.RegionSummary;
import com.sunz.hidden_travel.service.DummyRegionData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지역 정보 JSON API.
 * 지도에서 시군구를 클릭하면 SIG_CD 로 이 엔드포인트를 호출해 패널을 채운다.
 * (현재는 DB 없이 DummyRegionData 의 더미 응답)
 */
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final DummyRegionData data;

    public RegionController(DummyRegionData data) {
        this.data = data;
    }

    /** GET /api/regions/{sigCd} → 해당 지역 RegionSummary (JSON) */
    @GetMapping("/{sigCd}")
    public RegionSummary region(@PathVariable String sigCd) {
        return data.get(sigCd);
    }
}
