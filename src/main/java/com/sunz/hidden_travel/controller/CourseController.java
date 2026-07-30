package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.CoursePageData;
import com.sunz.hidden_travel.controller.dto.Recommendation;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.domain.SavedCourse;
import com.sunz.hidden_travel.domain.SavedCourseStop;
import com.sunz.hidden_travel.repository.RegionRepository;
import com.sunz.hidden_travel.service.RegionQueryService;
import com.sunz.hidden_travel.service.SavedCourseService;
import com.sunz.hidden_travel.user.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 내 코스 만들기 / 저장 완료 화면.
 * 후보 목록은 실데이터(Attraction/FoodPlace/GoodPriceShop/Specialty)를 sigCd로 조회해 채운다.
 * 코스 편집은 클라이언트 상태(course.js)로 관리하고, 저장 시 {@link SavedCourse} 로 영속화한다.
 */
@Controller
public class CourseController {

    private static final String DEFAULT_SIG = "47170"; // 안동시

    private final RegionQueryService regionQueryService;
    private final RegionRepository regionRepository;
    private final SavedCourseService savedCourseService;
    private final CurrentUserService currentUserService;

    public CourseController(RegionQueryService regionQueryService,
                            RegionRepository regionRepository,
                            SavedCourseService savedCourseService,
                            CurrentUserService currentUserService) {
        this.regionQueryService = regionQueryService;
        this.regionRepository = regionRepository;
        this.savedCourseService = savedCourseService;
        this.currentUserService = currentUserService;
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

    /** 코스 저장 → DB 영속화 후 저장 완료 화면으로 */
    @PostMapping("/course/save")
    public String save(@RequestParam(required = false) String sigCd,
                       @RequestParam(required = false) String courseName,
                       @RequestParam(required = false) String itemsJson) {
        String cd = sigCd != null ? sigCd : DEFAULT_SIG;
        Long userId = currentUserService.currentId();
        SavedCourse saved = savedCourseService.save(userId, cd, courseName, itemsJson);
        if (saved == null) {
            // 경유지가 없거나 파싱 실패 → 편집 화면으로 되돌린다
            return "redirect:/course?sigCd=" + cd;
        }
        return "redirect:/course/saved?courseId=" + saved.getId();
    }

    /** 코스 저장 완료 — 저장된 코스를 DB 에서 읽어 렌더 */
    @GetMapping("/course/saved")
    public String saved(@RequestParam(required = false) Long courseId, Model model) {
        SavedCourse course = savedCourseService.find(courseId);
        // 없는 코스이거나 남의 코스면 목록으로 (id 만 바꿔 남의 코스를 열어보지 못하게)
        if (course == null || !course.getUserId().equals(currentUserService.currentId())) {
            return "redirect:/my/courses";
        }

        Region region = regionRepository.findById(course.getSigCd()).orElse(null);
        String regionLabel = region != null ? (region.getProvince() + " " + region.getName()) : "";

        model.addAttribute("course", course);
        model.addAttribute("stopNames", course.getStops().stream().map(SavedCourseStop::getName).toList());
        model.addAttribute("regionLabel", regionLabel);
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
}
