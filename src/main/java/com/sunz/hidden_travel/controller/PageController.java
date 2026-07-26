package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.CourseCard;
import com.sunz.hidden_travel.controller.dto.CourseStop;
import com.sunz.hidden_travel.controller.dto.Recommendation;
import com.sunz.hidden_travel.controller.dto.RegionBundle;
import com.sunz.hidden_travel.controller.dto.RegionMetric;
import com.sunz.hidden_travel.controller.dto.RegionSummary;
import com.sunz.hidden_travel.service.DummyRegionData;
import com.sunz.hidden_travel.service.RegionQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 화면 라우팅 컨트롤러.
 * 지역 상세/패널은 {@link RegionQueryService}(DB 실데이터)로 채운다.
 * 코스 편집/저장 등 아직 데이터가 없는 화면은 더미({@link DummyRegionData})를 유지한다.
 * 기본 지역은 안동시(SIG_CD 47170).
 */
@Controller
public class PageController {

    private static final String DEFAULT_SIG = "47170"; // 안동시

    private final DummyRegionData regionData;
    private final RegionQueryService regionQueryService;

    public PageController(DummyRegionData regionData, RegionQueryService regionQueryService) {
        this.regionData = regionData;
        this.regionQueryService = regionQueryService;
    }

    /* =========================================================
       코스 편집/저장 화면 전용 더미
       ========================================================= */

    private List<CourseStop> myCourse() {
        return List.of(
                new CourseStop(1, "안동 하회마을", "명소", "10:00", false, "차로 25분 (15km)", "directions_car"),
                new CourseStop(2, "도산서원", "명소", "12:30", false, "차로 15분 (8km)", "directions_car"),
                new CourseStop(3, "안동댐 매운탕", "식당", "14:00", true, "도보 10분", "directions_walk"),
                new CourseStop(4, "낙강물길공원", "자연", "15:30", false, null, null)
        );
    }

    private List<Recommendation> nextRecommendations() {
        return List.of(
                new Recommendation("경북 의성군", "조용한 산사 산책", "사람 없는 고요한 사찰과 솔숲길"),
                new Recommendation("경북 영양군", "별빛 흐르는 밤", "국제밤하늘보호공원의 은하수"),
                new Recommendation("경북 봉화군", "오지 간이역 여행", "세월이 멈춘 산골 기차역")
        );
    }

    private RegionSummary toSummary(RegionBundle b) {
        return new RegionSummary(b.name(), b.province(), b.aiSummary(),
                b.specialties(), b.shops(), b.briefCourse());
    }

    /* =========================================================
       라우팅
       ========================================================= */

    /** 로그인 (헤더/푸터 숨김) */
    @GetMapping("/")
    public String login() {
        return "login";
    }

    /** 회원가입 (헤더/푸터 숨김) */
    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    /** 온보딩 (헤더/푸터 숨김) */
    @GetMapping("/onboarding")
    public String onboarding() {
        return "onboarding";
    }

    /** 메인 탐색(지도) — 헤더/푸터 프래그먼트 사용 */
    @GetMapping("/map")
    public String map(Model model) {
        model.addAttribute("regionOptions", regionData.options());
        return "map";
    }

    /** 지역 상세 탐색 — 지도 우측 슬라이드 패널(독립 페이지 버전, 실데이터) */
    @GetMapping("/region/panel")
    public String regionPanel(Model model) {
        model.addAttribute("region", toSummary(regionQueryService.bundle(DEFAULT_SIG)));
        return "region-panel";
    }

    /** 지역 상세(깊이 있는 탐색) — 전체 페이지, 실데이터 */
    @GetMapping("/region")
    public String regionDetail(@RequestParam(value = "sigCd", required = false) String sigCd, Model model) {
        RegionBundle b = regionQueryService.bundle(sigCd != null ? sigCd : DEFAULT_SIG);
        model.addAttribute("region", toSummary(b));
        model.addAttribute("heroDesc", b.aiSummary());
        model.addAttribute("metrics", List.of(
                new RegionMetric(String.valueOf(b.attractionCount()), "관광 콘텐츠 수"),
                new RegionMetric(String.valueOf(b.foodCount()), "맛집 수"),
                new RegionMetric(String.valueOf(b.shopCount()), "착한가격업소 수"),
                new RegionMetric(String.valueOf(b.specialtyCount()), "특산물 수")
        ));
        model.addAttribute("recommendedCourses", b.recommendedCourses());
        return "region-detail";
    }

    /** 내 코스 만들기 */
    @GetMapping("/course")
    public String course(Model model) {
        List<CourseStop> stops = myCourse();
        model.addAttribute("region", regionData.get(DEFAULT_SIG));
        model.addAttribute("courseName", "안동 하루 코스");
        model.addAttribute("stops", stops);
        model.addAttribute("totalPlaces", stops.size());
        model.addAttribute("totalTime", "약 6시간");
        model.addAttribute("totalDistance", "32km");
        return "course";
    }

    /** 코스 저장 완료 (헤더/푸터 숨김) */
    @GetMapping("/course/saved")
    public String courseSaved(Model model) {
        CourseCard saved = new CourseCard(
                "천년 고도에서 보내는 하루", "역사·미식", "3시간 30분",
                List.of("안동 하회마을", "도산서원", "안동댐 매운탕", "낙강물길공원"), "12.5km");
        model.addAttribute("savedCourse", saved);
        model.addAttribute("regionLabel", "경상북도 안동시");
        model.addAttribute("totalPlaces", saved.points().size());
        model.addAttribute("goodPriceCount", 2);
        model.addAttribute("recommendations", nextRecommendations());
        return "course-saved";
    }
}
