package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.RecommendRequest;
import com.sunz.hidden_travel.controller.dto.RecommendResult;
import com.sunz.hidden_travel.recommend.RecommendService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 추천 API. POST /api/recommend → 추천 지역 { sigCd, name }.
 * (사용자 입력을 받아 RecommendService 에 위임 — 현재는 랜덤, 이후 AI로 교체)
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping
    public RecommendResult recommend(@RequestBody(required = false) RecommendRequest request) {
        RecommendRequest req = request != null ? request : new RecommendRequest(List.of(), "", "");
        return recommendService.recommend(req);
    }
}
