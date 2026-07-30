package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.SpotlightCard;
import com.sunz.hidden_travel.domain.Attraction;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.domain.Specialty;
import com.sunz.hidden_travel.repository.AttractionRepository;
import com.sunz.hidden_travel.repository.GoodPriceShopRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import com.sunz.hidden_travel.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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

    /** 소개 문구에 이름을 나열할 관광지 수 */
    private static final int NAMES_IN_DESC = 3;

    private final RegionRepository regionRepository;
    private final AttractionRepository attractionRepository;
    private final GoodPriceShopRepository goodPriceShopRepository;
    private final SpecialtyRepository specialtyRepository;

    public SpotlightService(RegionRepository regionRepository,
                            AttractionRepository attractionRepository,
                            GoodPriceShopRepository goodPriceShopRepository,
                            SpecialtyRepository specialtyRepository) {
        this.regionRepository = regionRepository;
        this.attractionRepository = attractionRepository;
        this.goodPriceShopRepository = goodPriceShopRepository;
        this.specialtyRepository = specialtyRepository;
    }

    /**
     * 서로 다른 지역 카드를 count 장 뽑는다.
     *
     * @param exclude 이미 보여준 시군구 (무한 스크롤에서 중복 방지)
     */
    @Transactional(readOnly = true)
    public List<SpotlightCard> pick(int count, Collection<String> exclude) {
        Set<String> skip = exclude == null ? Set.of() : Set.copyOf(exclude);
        List<String> candidates = new ArrayList<>(
                attractionRepository.findHiddenCandidateSigCds(EXCLUDED_SIDO, MIN_ATTRACTIONS));
        candidates.removeAll(skip);

        List<SpotlightCard> cards = new ArrayList<>();
        while (!candidates.isEmpty() && cards.size() < count) {
            String sigCd = candidates.remove(ThreadLocalRandom.current().nextInt(candidates.size()));
            SpotlightCard card = toCard(sigCd);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    public List<SpotlightCard> pick(int count) {
        return pick(count, Set.of());
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

        List<Attraction> all = attractionRepository.findBySigCd(sigCd);
        int shopCount = goodPriceShopRepository.findBySigCd(sigCd).size();

        return new SpotlightCard(
                sigCd,
                region.getName(),
                region.getProvince(),
                hero.getImage(),
                hero.getName(),
                describe(sigCd, hero, all),
                all.size(),
                shopCount);
    }

    /**
     * 지역 소개 문구.
     *
     * TourAPI 상세(overview)는 콘텐츠 1건당 호출 2회가 들어 카드마다 부르면 한도를
     * 금방 소진한다. 그래서 <b>이미 적재된 데이터만으로</b> 문장을 만든다.
     * 예) "회룡포, 삼강주막 등을 볼 수 있고, 사과·참깨가 유명해요."
     */
    private String describe(String sigCd, Attraction hero, List<Attraction> all) {
        List<String> names = new ArrayList<>();
        names.add(hero.getName());
        for (Attraction a : all) {
            if (names.size() >= NAMES_IN_DESC) {
                break;
            }
            if (a.getName() != null && !names.contains(a.getName())) {
                names.add(a.getName());
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!names.isEmpty()) {
            sb.append(String.join(", ", names)).append(" 등을 볼 수 있어요.");
        }

        List<String> specialties = specialtyRepository.findBySigCd(sigCd).stream()
                .map(Specialty::getName)
                .filter(n -> n != null && !n.isBlank())
                .limit(2)
                .toList();
        if (!specialties.isEmpty()) {
            sb.append(' ').append(String.join("·", specialties)).append("이(가) 유명합니다.");
        }
        return sb.toString().trim();
    }
}
