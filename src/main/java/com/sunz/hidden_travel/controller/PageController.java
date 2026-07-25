package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.CourseCard;
import com.sunz.hidden_travel.controller.dto.CoursePoint;
import com.sunz.hidden_travel.controller.dto.CourseStop;
import com.sunz.hidden_travel.controller.dto.GoodPriceShop;
import com.sunz.hidden_travel.controller.dto.Recommendation;
import com.sunz.hidden_travel.controller.dto.RegionMetric;
import com.sunz.hidden_travel.controller.dto.RegionSummary;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 화면 라우팅 + 퍼블리싱용 더미 데이터 주입 컨트롤러.
 * (DB / JPA / Security / 외부 API 연동 없음 — 하드코딩 더미 데이터)
 *
 * 화면 흐름이 자연스럽게 이어지도록 모든 지역 더미 데이터는 "안동시" 하나로 통일한다.
 */
@Controller
public class PageController {

    /* =========================================================
       더미 데이터 (안동시)
       ========================================================= */

    private RegionSummary andong() {
        return new RegionSummary(
                "안동시",
                "경상북도",
                "낙동강이 휘감아 도는 하회마을과 유교 문화의 정수를 간직한 도시. "
                        + "붐비지 않는 골목마다 전통과 미식이 조용히 숨어 있습니다.",
                List.of("안동간고등어", "안동찜닭", "안동소주", "헛제사밥", "안동포"),
                goodPriceShops(),
                briefCourse()
        );
    }

    /** 착한가격업소 (region-detail 6곳 / region-panel 은 앞 3곳만 노출) */
    private List<GoodPriceShop> goodPriceShops() {
        return List.of(
                new GoodPriceShop("소문난 국밥집", "돼지국밥", "7,000원", "한식", "안동시 옥동 123-4"),
                new GoodPriceShop("할매 떡볶이", "떡볶이 1인분", "3,000원", "분식", "안동시 중앙로 45"),
                new GoodPriceShop("만리장성", "짜장면", "5,000원", "중식", "안동시 태화동 78"),
                new GoodPriceShop("정가네 보리밥", "보리밥 정식", "8,000원", "한식", "안동시 평화동 22"),
                new GoodPriceShop("다방 연", "아메리카노", "2,500원", "카페", "안동시 삼산동 15"),
                new GoodPriceShop("시장 손칼국수", "손칼국수", "6,000원", "한식", "안동시 신시장 내")
        );
    }

    /** region-panel "추천 반일 코스" 타임라인 */
    private List<CoursePoint> briefCourse() {
        return List.of(
                new CoursePoint(1, "하회마을", "관광명소",
                        "유네스코 세계문화유산, 낙동강이 감싸 안은 전통 마을 산책", "10:00"),
                new CoursePoint(2, "안동구시장", "전통시장",
                        "안동찜닭 골목에서 향토 음식 맛보기", "12:00"),
                new CoursePoint(3, "월영교", "자연/경관",
                        "국내 최장 목책 인도교에서 달빛 산책", "17:00")
        );
    }

    /** region-detail "추천 코스" 카드 3종 */
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

    /** region-detail 지표 스트립 */
    private List<RegionMetric> metrics() {
        return List.of(
                new RegionMetric("1.2M", "연간 방문객"),
                new RegionMetric("Top 5", "전국 랭킹"),
                new RegionMetric("342", "관광 콘텐츠 수"),
                new RegionMetric("45", "착한가격업소 수")
        );
    }

    /** course 편집기 "나의 코스" 타임라인 */
    private List<CourseStop> myCourse() {
        return List.of(
                new CourseStop(1, "안동 하회마을", "명소", "10:00", false, "차로 25분 (15km)", "directions_car"),
                new CourseStop(2, "도산서원", "명소", "12:30", false, "차로 15분 (8km)", "directions_car"),
                new CourseStop(3, "안동댐 매운탕", "식당", "14:00", true, "도보 10분", "directions_walk"),
                new CourseStop(4, "낙강물길공원", "자연", "15:30", false, null, null)
        );
    }

    /** course-saved "다음엔 어디로?" 추천 (map 검색 드롭다운 지역과 연결되는 숨은 여행지) */
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
        model.addAttribute("regionNames", List.of("의성군", "영양군", "봉화군"));
        return "map";
    }

    /** 지역 상세 탐색 — 지도 우측 슬라이드 패널 */
    @GetMapping("/region/panel")
    public String regionPanel(Model model) {
        model.addAttribute("region", andong());
        return "region-panel";
    }

    /** 지역 상세(깊이 있는 탐색) — 전체 페이지, 헤더/푸터 프래그먼트 사용 */
    @GetMapping("/region")
    public String regionDetail(Model model) {
        RegionSummary region = andong();
        model.addAttribute("region", region);
        model.addAttribute("heroDesc",
                "시간이 머무는 곳, 전통과 자연이 조화롭게 숨 쉬는 안동에서 잊혀진 가치를 발견해보세요.");
        model.addAttribute("metrics", metrics());
        model.addAttribute("recommendedCourses", recommendedCourses());
        return "region-detail";
    }

    /** 내 코스 만들기 */
    @GetMapping("/course")
    public String course(Model model) {
        List<CourseStop> stops = myCourse();
        model.addAttribute("region", andong());
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
