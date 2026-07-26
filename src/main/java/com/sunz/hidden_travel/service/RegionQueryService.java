package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.CandidateItem;
import com.sunz.hidden_travel.controller.dto.CourseCard;
import com.sunz.hidden_travel.controller.dto.CourseInitItem;
import com.sunz.hidden_travel.controller.dto.CoursePageData;
import com.sunz.hidden_travel.controller.dto.CoursePoint;
import com.sunz.hidden_travel.controller.dto.GoodPriceShop;
import com.sunz.hidden_travel.controller.dto.RegionBundle;
import com.sunz.hidden_travel.domain.Attraction;
import com.sunz.hidden_travel.domain.FoodPlace;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.domain.Specialty;
import com.sunz.hidden_travel.domain.TravelCourse;
import com.sunz.hidden_travel.repository.AttractionRepository;
import com.sunz.hidden_travel.repository.FoodPlaceRepository;
import com.sunz.hidden_travel.repository.GoodPriceShopRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import com.sunz.hidden_travel.repository.SpecialtyRepository;
import com.sunz.hidden_travel.repository.TravelCourseRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SIG_CD 기준으로 DB 실데이터를 조립해 지역 화면(패널/상세)에 제공한다.
 * (DummyRegionData 를 대체하는 실데이터 조회 서비스)
 */
@Service
public class RegionQueryService {

    private static final int SHOP_LIMIT = 6;
    private static final int COURSE_CARD_LIMIT = 3;
    private static final int CANDIDATE_LIMIT = 40;

    /** 착한가격업소 중 비식당(서비스) 업종 제외 키워드 — 화면은 식당 위주로 노출 */
    private static final List<String> NON_FOOD = List.of(
            "비요식", "미용", "이용", "이미용", "세탁", "목욕", "숙박", "여관", "안경", "사진",
            "인쇄", "노래", "학원", "자동차", "수리", "헤어", "네일", "피부", "화장", "서비스"
    );

    /** TourAPI 여행코스 cat3 코드 → 한글 라벨 */
    private static final Map<String, String> COURSE_THEME = Map.ofEntries(
            Map.entry("C0112", "가족 코스"), Map.entry("C0113", "나홀로 코스"),
            Map.entry("C0114", "힐링 코스"), Map.entry("C0115", "캠핑 코스"),
            Map.entry("C0116", "맛 코스"), Map.entry("C0117", "1박 2일 코스"),
            Map.entry("C0118", "낭만 코스"), Map.entry("C0119", "인생샷 코스"),
            Map.entry("C0120", "등산 코스"), Map.entry("C0121", "트레킹 코스")
    );

    private final RegionRepository regionRepository;
    private final AttractionRepository attractionRepository;
    private final FoodPlaceRepository foodPlaceRepository;
    private final GoodPriceShopRepository goodPriceShopRepository;
    private final SpecialtyRepository specialtyRepository;
    private final TravelCourseRepository travelCourseRepository;

    public RegionQueryService(RegionRepository regionRepository,
                              AttractionRepository attractionRepository,
                              FoodPlaceRepository foodPlaceRepository,
                              GoodPriceShopRepository goodPriceShopRepository,
                              SpecialtyRepository specialtyRepository,
                              TravelCourseRepository travelCourseRepository) {
        this.regionRepository = regionRepository;
        this.attractionRepository = attractionRepository;
        this.foodPlaceRepository = foodPlaceRepository;
        this.goodPriceShopRepository = goodPriceShopRepository;
        this.specialtyRepository = specialtyRepository;
        this.travelCourseRepository = travelCourseRepository;
    }

    public RegionBundle bundle(String sigCd) {
        Region region = regionRepository.findById(sigCd).orElse(null);
        String name = region != null ? region.getName() : "알 수 없는 지역";
        String province = region != null ? region.getProvince() : "";

        List<Attraction> attractions = attractionRepository.findBySigCd(sigCd);
        List<FoodPlace> foods = foodPlaceRepository.findBySigCd(sigCd);
        List<com.sunz.hidden_travel.domain.GoodPriceShop> shops = goodPriceShopRepository.findBySigCd(sigCd);
        List<Specialty> specialties = specialtyRepository.findBySigCd(sigCd);
        List<TravelCourse> courses = travelCourseRepository.findBySigCd(sigCd);

        int attractionCount = attractions.size();
        int foodCount = foods.size();
        int shopCount = shops.size();
        int specialtyCount = specialties.size();
        boolean dataReady = attractionCount + foodCount + shopCount + specialtyCount > 0;

        String aiSummary = dataReady
                ? String.format("%s %s · 관광지 %d곳, 착한가격업소 %d곳, 특산물 %d종이 기다리는 곳입니다.",
                        province, name, attractionCount, shopCount, specialtyCount)
                : "이 지역의 여행 정보는 아직 준비 중이에요. 조금만 기다려 주세요.";

        List<String> specialtyNames = specialties.stream().map(Specialty::getName).toList();

        // 식당 위주로 노출(식당이 없으면 전체로 폴백)
        List<com.sunz.hidden_travel.domain.GoodPriceShop> foodShops = shops.stream()
                .filter(s -> isFood(s.getCategory()))
                .toList();
        List<com.sunz.hidden_travel.domain.GoodPriceShop> displayShops = foodShops.isEmpty() ? shops : foodShops;
        List<GoodPriceShop> shopDtos = displayShops.stream()
                .limit(SHOP_LIMIT)
                .map(s -> new GoodPriceShop(s.getName(), s.getMenu(), priceText(s.getPrice()),
                        s.getCategory(), s.getAddr()))
                .toList();

        return new RegionBundle(sigCd, name, province, dataReady, aiSummary,
                specialtyNames, shopDtos, briefCourse(courses, attractions, foods),
                recommendedCourses(name, courses, attractions, foods),
                attractionCount, foodCount, shopCount, specialtyCount);
    }

    /* 패널 "추천 반일 코스": 여행코스 경유지 우선, 없으면 관광지+맛집 간이 조합 */
    private List<CoursePoint> briefCourse(List<TravelCourse> courses, List<Attraction> attractions, List<FoodPlace> foods) {
        List<CoursePoint> brief = new ArrayList<>();
        TravelCourse withPoints = courses.stream().filter(c -> !c.getPoints().isEmpty()).findFirst().orElse(null);
        if (withPoints != null) {
            int order = 1;
            for (com.sunz.hidden_travel.domain.CoursePoint p : withPoints.getPoints()) {
                brief.add(new CoursePoint(order++, p.getName(),
                        p.getType() != null ? p.getType() : "코스", p.getDescription(), null));
                if (order > 4) break;
            }
            return brief;
        }
        int order = 1;
        for (Attraction a : attractions) {
            brief.add(new CoursePoint(order++, a.getName(), "관광지", a.getAddr(), null));
            if (order > 3) break;
        }
        if (!foods.isEmpty()) {
            brief.add(new CoursePoint(order, foods.get(0).getName(), "맛집", foods.get(0).getAddr(), null));
        }
        return brief;
    }

    /* 상세 "추천 코스" 카드: TravelCourse 우선, 없으면 간이 코스 1개 */
    private List<CourseCard> recommendedCourses(String name, List<TravelCourse> courses,
                                                List<Attraction> attractions, List<FoodPlace> foods) {
        List<CourseCard> cards = new ArrayList<>();
        for (TravelCourse tc : courses) {
            if (cards.size() >= COURSE_CARD_LIMIT) break;
            List<String> points = tc.getPoints().stream()
                    .map(com.sunz.hidden_travel.domain.CoursePoint::getName)
                    .limit(6).toList();
            cards.add(new CourseCard(tc.getTitle(), themeLabel(tc.getTheme()), "여행코스",
                    points, tc.getTotalDistance(), tc.getId()));
        }
        if (cards.isEmpty() && (!attractions.isEmpty() || !foods.isEmpty())) {
            List<String> points = new ArrayList<>();
            attractions.stream().limit(3).forEach(a -> points.add(a.getName()));
            foods.stream().limit(1).forEach(f -> points.add(f.getName()));
            cards.add(new CourseCard(name + " 하루 한 바퀴", "추천 코스", "간이 코스", points, null, null));
        }
        return cards;
    }

    /* =========================================================
       코스 만들기 화면 데이터 (후보 4탭 + 초기 코스)
       ========================================================= */
    public CoursePageData coursePageData(String sigCd, Long courseId) {
        Region region = regionRepository.findById(sigCd).orElse(null);
        String regionName = region != null ? region.getName() : "지역";

        List<CandidateItem> attractions = attractionRepository.findBySigCd(sigCd).stream()
                .limit(CANDIDATE_LIMIT)
                .map(a -> new CandidateItem(String.valueOf(a.getId()), "attraction", a.getName(),
                        a.getAddr(), a.getType() != null ? a.getType() : "관광지", false, null))
                .toList();

        List<CandidateItem> foods = foodPlaceRepository.findBySigCd(sigCd).stream()
                .limit(CANDIDATE_LIMIT)
                .map(f -> new CandidateItem(String.valueOf(f.getId()), "food", f.getName(),
                        f.getAddr(), f.getCategory() != null ? f.getCategory() : "먹거리", false, null))
                .toList();

        List<CandidateItem> goodShops = goodPriceShopRepository.findBySigCd(sigCd).stream()
                .filter(s -> isFood(s.getCategory()))
                .limit(CANDIDATE_LIMIT)
                .map(s -> new CandidateItem(String.valueOf(s.getId()), "goodprice", s.getName(),
                        s.getAddr(), s.getCategory(), true, shopPriceText(s)))
                .toList();

        List<CandidateItem> specialties = specialtyRepository.findBySigCd(sigCd).stream()
                .map(sp -> new CandidateItem(String.valueOf(sp.getId()), "specialty", sp.getName(),
                        sp.getSeason(), "특산물", false, null))
                .toList();

        List<CourseInitItem> initial = new ArrayList<>();
        if (courseId != null) {
            travelCourseRepository.findById(courseId).ifPresent(tc -> {
                int order = 1;
                for (com.sunz.hidden_travel.domain.CoursePoint p : tc.getPoints()) {
                    initial.add(new CourseInitItem(order++, p.getName(),
                            p.getType() != null ? p.getType() : "코스", false));
                }
            });
        }

        return new CoursePageData(sigCd, regionName, regionName + " 코스",
                attractions, foods, goodShops, specialties, initial);
    }

    private String shopPriceText(com.sunz.hidden_travel.domain.GoodPriceShop s) {
        String m = s.getMenu() != null ? s.getMenu() : "";
        return (m + " " + priceText(s.getPrice())).trim();
    }

    private String themeLabel(String cat) {
        if (cat == null) return "여행코스";
        return COURSE_THEME.getOrDefault(cat, "여행코스");
    }

    private boolean isFood(String category) {
        if (category == null) return true;
        return NON_FOOD.stream().noneMatch(category::contains);
    }

    private String priceText(Integer price) {
        return price == null ? "가격문의" : String.format("%,d원", price);
    }
}
