package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.SpotlightCard;
import com.sunz.hidden_travel.domain.Attraction;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.repository.AttractionRepository;
import com.sunz.hidden_travel.repository.GoodPriceShopRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 지도 화면의 '오늘의 숨은 여행지' 카드를 고른다.
 *
 * 선정 기준: 광역시·경기·제주를 제외하고, 보여줄 관광지가 충분히 쌓인 시군구.
 * 인기도 지표가 없어 "덜 알려진 곳"을 이렇게 근사한다.
 * (지수가 생기면 이 후보 산출만 교체하면 된다)
 */
@Service
public class SpotlightService {

    /** 이미 잘 알려져 추천 의미가 옅은 시도 — 특별시·광역시·경기·제주 */
    private static final List<String> EXCLUDED_SIDO =
            List.of("11", "26", "27", "28", "29", "30", "31", "36", "41", "50");

    /** 카드로 내보낼 최소 관광지 수 — 너무 적으면 눌러도 볼 게 없다 */
    private static final long MIN_ATTRACTIONS = 10;

    private final RegionRepository regionRepository;
    private final AttractionRepository attractionRepository;
    private final GoodPriceShopRepository goodPriceShopRepository;

    public SpotlightService(RegionRepository regionRepository,
                            AttractionRepository attractionRepository,
                            GoodPriceShopRepository goodPriceShopRepository) {
        this.regionRepository = regionRepository;
        this.attractionRepository = attractionRepository;
        this.goodPriceShopRepository = goodPriceShopRepository;
    }

    /** 서로 다른 지역 카드를 count 장 뽑는다. 후보가 모자라면 그만큼만 반환. */
    @Transactional(readOnly = true)
    public List<SpotlightCard> pick(int count) {
        List<String> candidates =
                new ArrayList<>(attractionRepository.findHiddenCandidateSigCds(EXCLUDED_SIDO, MIN_ATTRACTIONS));
        List<SpotlightCard> cards = new ArrayList<>();
        if (candidates.isEmpty()) {
            return cards;
        }

        while (!candidates.isEmpty() && cards.size() < count) {
            String sigCd = candidates.remove(ThreadLocalRandom.current().nextInt(candidates.size()));
            SpotlightCard card = toCard(sigCd);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    private SpotlightCard toCard(String sigCd) {
        Region region = regionRepository.findById(sigCd).orElse(null);
        if (region == null) {
            return null;
        }
        List<Attraction> withImage = attractionRepository.findBySigCdAndImageIsNotNull(sigCd);
        if (withImage.isEmpty()) {
            return null;   // 사진이 없으면 카드로서 매력이 없다
        }
        // 매번 같은 사진이 나오지 않도록 무작위로 고른다
        Attraction hero = withImage.get(ThreadLocalRandom.current().nextInt(withImage.size()));

        return new SpotlightCard(
                sigCd,
                region.getName(),
                region.getProvince(),
                hero.getImage(),
                hero.getName(),
                attractionRepository.findBySigCd(sigCd).size(),
                goodPriceShopRepository.findBySigCd(sigCd).size());
    }
}
