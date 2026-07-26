package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.CourseCard;
import com.sunz.hidden_travel.controller.dto.CoursePageData;
import com.sunz.hidden_travel.controller.dto.Recommendation;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.repository.RegionRepository;
import com.sunz.hidden_travel.service.RegionQueryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 내 코스 만들기 / 저장 완료 화면.
 * 실데이터(Attraction/FoodPlace/GoodPriceShop/Specialty)를 sigCd로 조회해 채운다.
 * 코스 편집은 클라이언트 상태(course.js)로 관리하고, 저장 시 요약을 course-saved 로 넘긴다.
 */
@Controller
public class CourseController {

    private static final String DEFAULT_SIG = "47170"; // 안동시

    private final RegionQueryService regionQueryService;
    private final RegionRepository regionRepository;

    public CourseController(RegionQueryService regionQueryService, RegionRepository regionRepository) {
        this.regionQueryService = regionQueryService;
        this.regionRepository = regionRepository;
    }

    /** 내 코스 만들기 — sigCd 후보 데이터 + (courseId) 초기 코스 */
    @GetMapping("/course")
    public String course(@RequestParam(required = false) String sigCd,
                         @RequestParam(required = false) Long courseId,
                         Model model) {
        String cd = sigCd != null ? sigCd : DEFAULT_SIG;
        CoursePageData data = regionQueryService.coursePageData(cd, courseId);
        model.addAttribute("data", data);
        return "course";
    }

    /** 코스 저장(최소 구현) — 요약을 flash 로 넘겨 course-saved 표시 */
    @PostMapping("/course/save")
    public String save(@RequestParam(required = false) String sigCd,
                       @RequestParam(required = false) String courseName,
                       @RequestParam(defaultValue = "0") int totalPlaces,
                       @RequestParam(defaultValue = "0") int goodPriceCount,
                       @RequestParam(required = false) String itemNames,
                       RedirectAttributes ra) {
        ra.addFlashAttribute("sigCd", sigCd != null ? sigCd : DEFAULT_SIG);
        ra.addFlashAttribute("savedCourseName", courseName != null ? courseName : "나의 코스");
        ra.addFlashAttribute("totalPlaces", totalPlaces);
        ra.addFlashAttribute("goodPriceCount", goodPriceCount);
        ra.addFlashAttribute("itemNames", itemNames != null ? itemNames : "");
        return "redirect:/course/saved";
    }

    /** 코스 저장 완료 — flash 요약으로 렌더(직접 접근 시 기본값) */
    @GetMapping("/course/saved")
    public String saved(Model model) {
        String sigCd = str(model.getAttribute("sigCd"), DEFAULT_SIG);
        String courseName = str(model.getAttribute("savedCourseName"), "나의 코스");
        int totalPlaces = intv(model.getAttribute("totalPlaces"));
        int goodPriceCount = intv(model.getAttribute("goodPriceCount"));
        String itemNames = str(model.getAttribute("itemNames"), "");

        Region region = regionRepository.findById(sigCd).orElse(null);
        String regionLabel = region != null ? (region.getProvince() + " " + region.getName()) : "";

        List<String> points = itemNames.isBlank()
                ? List.of()
                : Arrays.stream(itemNames.split("\\|")).filter(s -> !s.isBlank()).toList();

        // 소요시간/이동거리는 동선 계산 전이라 미표시("-")
        CourseCard saved = new CourseCard(courseName, "나의 코스", "-", points, "-", null);
        model.addAttribute("savedCourse", saved);
        model.addAttribute("regionLabel", regionLabel);
        model.addAttribute("totalPlaces", totalPlaces);
        model.addAttribute("goodPriceCount", goodPriceCount);
        model.addAttribute("recommendations", nextRecommendations());
        return "course-saved";
    }

    private List<Recommendation> nextRecommendations() {
        return List.of(
                new Recommendation("경북 의성군", "조용한 산사 산책", "사람 없는 고요한 사찰과 솔숲길"),
                new Recommendation("경북 영양군", "별빛 흐르는 밤", "국제밤하늘보호공원의 은하수"),
                new Recommendation("경북 봉화군", "오지 간이역 여행", "세월이 멈춘 산골 기차역")
        );
    }

    private String str(Object o, String def) {
        return o != null ? o.toString() : def;
    }

    private int intv(Object o) {
        return o instanceof Integer i ? i : 0;
    }
}
