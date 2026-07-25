package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.CourseCard;
import com.sunz.hidden_travel.controller.dto.CoursePoint;
import com.sunz.hidden_travel.controller.dto.CourseStop;
import com.sunz.hidden_travel.controller.dto.Recommendation;
import com.sunz.hidden_travel.controller.dto.RegionMetric;
import com.sunz.hidden_travel.controller.dto.RegionSummary;
import com.sunz.hidden_travel.service.DummyRegionData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 화면 라우팅 + 퍼블리싱용 더미 데이터 주입 컨트롤러.
 * (DB / JPA / Security / 외부 API 연동 없음 — 하드코딩 더미 데이터)
 *
 * 지역 더미 데이터는 {@link DummyRegionData} 로 중앙화하여 API/페이지가 공유한다.
 * 기본 지역은 안동시(SIG_CD 47170).
 */
@Controller
public class PageController {

    private static final String DEFAULT_SIG = "47170"; // 안동시

    private final DummyRegionData regionData;

    public PageController(DummyRegionData regionData) {
        this.regionData = regionData;
    }

    /* =========================================================
       페이지 전용 더미 (코스/지표/추천)
       ========================================================= */

    private List<CourseCard> recommendedCourses() {
        return List.of(
                new CourseCard("천년의 발자취를 따라서", "역사 탐방", "반나절",
                        List.of("하회마을", "병산서원", "도산서원"), "12km"),
                new CourseCard("입이 즐거운 안동 한 바퀴", "미식 여행", "당일치기",
                        List.of("안동 구시장 (찜닭 골목)", "맘모스 베이커리", "월영교 달빛 산책", "헛제사밥 거리"), "8km"),
                new CourseCard("물길 따라 걷는 사색의 시간", "자연 휴양", "1박 2일",
                        List.of("낙동강 생태 학습관", "선성수상길", "만휴정"), "25km")
        );
    }

    private List<RegionMetric> metrics() {
        return List.of(
                new RegionMetric("1.2M", "연간 방문객"),
                new RegionMetric("Top 5", "전국 랭킹"),
                new RegionMetric("342", "관광 콘텐츠 수"),
                new RegionMetric("45", "착한가격업소 수")
        );
    }

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

    /** 지역 상세 탐색 — 지도 우측 슬라이드 패널(독립 페이지 버전) */
    @GetMapping("/region/panel")
    public String regionPanel(Model model) {
        model.addAttribute("region", regionData.get(DEFAULT_SIG));
        return "region-panel";
    }

    /** 지역 상세(깊이 있는 탐색) — 전체 페이지, 헤더/푸터 프래그먼트 사용 */
    @GetMapping("/region")
    public String regionDetail(@RequestParam(value = "sigCd", required = false) String sigCd, Model model) {
        RegionSummary region = regionData.get(sigCd != null ? sigCd : DEFAULT_SIG);
        model.addAttribute("region", region);
        model.addAttribute("heroDesc", region.aiSummary());
        model.addAttribute("metrics", metrics());
        model.addAttribute("recommendedCourses", recommendedCourses());
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
