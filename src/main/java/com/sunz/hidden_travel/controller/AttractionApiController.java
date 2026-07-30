package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.AttractionDetail;
import com.sunz.hidden_travel.service.AttractionDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관광지 상세 조회 API. 코스 만들기 화면에서 카드를 펼칠 때 호출한다.
 * 최초 1회만 TourAPI 를 타고 이후에는 DB 캐시에서 나간다.
 */
@RestController
public class AttractionApiController {

    private final AttractionDetailService attractionDetailService;

    public AttractionApiController(AttractionDetailService attractionDetailService) {
        this.attractionDetailService = attractionDetailService;
    }

    @GetMapping("/api/attraction/{id}")
    public ResponseEntity<AttractionDetail> detail(@PathVariable Long id) {
        AttractionDetail detail = attractionDetailService.detail(id);
        return detail == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(detail);
    }

    /**
     * TourAPI contentId 로 조회 — 여행코스 경유지용.
     * 아직 적재되지 않은 콘텐츠면 관광지로 새로 저장한 뒤 돌려준다.
     */
    @GetMapping("/api/attraction/by-content/{contentId}")
    public ResponseEntity<AttractionDetail> detailByContent(@PathVariable String contentId) {
        AttractionDetail detail = attractionDetailService.detailByContentId(contentId);
        return detail == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(detail);
    }
}
