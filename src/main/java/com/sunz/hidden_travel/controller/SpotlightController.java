package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.SpotlightCard;
import com.sunz.hidden_travel.service.SpotlightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * '오늘의 숨은 여행지' 카드 API — 지도 화면의 [다른 곳 보기]가 호출한다.
 */
@RestController
public class SpotlightController {

    private static final int MAX_COUNT = 6;

    private final SpotlightService spotlightService;

    public SpotlightController(SpotlightService spotlightService) {
        this.spotlightService = spotlightService;
    }

    /**
     * @param exclude 이미 보여준 시군구 코드 (무한 스크롤에서 중복 방지)
     */
    @GetMapping("/api/spotlight")
    public List<SpotlightCard> spotlight(@RequestParam(defaultValue = "3") int count,
                                         @RequestParam(required = false) List<String> exclude) {
        return spotlightService.pick(Math.clamp(count, 1, MAX_COUNT), exclude);
    }
}
