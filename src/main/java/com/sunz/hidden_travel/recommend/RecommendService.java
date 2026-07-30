package com.sunz.hidden_travel.recommend;

import com.sunz.hidden_travel.controller.dto.RecommendRequest;
import com.sunz.hidden_travel.controller.dto.RecommendResult;

/**
 * 여행지 추천 서비스. 지금은 랜덤 구현이지만, 이후 AI 호출 구현으로 교체할 자리.
 */
public interface RecommendService {

    RecommendResult recommend(RecommendRequest request);
}
