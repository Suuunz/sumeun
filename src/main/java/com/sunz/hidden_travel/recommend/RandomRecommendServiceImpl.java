package com.sunz.hidden_travel.recommend;

import com.sunz.hidden_travel.controller.dto.RecommendRequest;
import com.sunz.hidden_travel.controller.dto.RecommendResult;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.repository.AttractionRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 랜덤 추천 구현. 단, "실제로 보여줄 데이터가 있는 지역"(관광지가 적재된 시군구) 중에서만 고른다.
 * 입력값(styles/mood/freeText)은 지금은 사용하지 않고 로그로만 보관 — 이후 AI 구현이 활용한다.
 *
 * (교체 방법: 이 구현 대신 AiRecommendServiceImpl 을 @Primary 로 두거나 이 빈을 교체)
 */
@Service
public class RandomRecommendServiceImpl implements RecommendService {

    private static final Logger log = LoggerFactory.getLogger(RandomRecommendServiceImpl.class);

    private final AttractionRepository attractionRepository;
    private final RegionRepository regionRepository;

    public RandomRecommendServiceImpl(AttractionRepository attractionRepository,
                                      RegionRepository regionRepository) {
        this.attractionRepository = attractionRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public RecommendResult recommend(RecommendRequest request) {
        List<String> sigCds = attractionRepository.findDistinctSigCd();
        if (sigCds.isEmpty()) {
            log.warn("[Recommend] 추천 후보(데이터 있는 지역)가 없습니다.");
            return new RecommendResult(null, null);
        }
        String sigCd = sigCds.get(ThreadLocalRandom.current().nextInt(sigCds.size()));
        String name = regionRepository.findById(sigCd).map(Region::getName).orElse(sigCd);

        // AI 교체를 위해 입력값 보관(현재 랜덤은 미사용)
        log.info("[Recommend] styles={}, mood={}, freeText='{}' → 추천 {} {}",
                request != null ? request.styles() : null,
                request != null ? request.mood() : null,
                request != null ? request.freeText() : null,
                sigCd, name);

        return new RecommendResult(sigCd, name);
    }
}
