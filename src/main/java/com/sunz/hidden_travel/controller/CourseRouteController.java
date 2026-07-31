package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.controller.dto.CourseRoute;
import com.sunz.hidden_travel.service.CourseRouteService;
import com.sunz.hidden_travel.service.SavedCourseService;
import com.sunz.hidden_travel.user.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 코스의 실제 도로 경로 API — 저장 완료 화면이 렌더 후 호출한다.
 * 첫 호출에서만 길찾기를 부르고 결과를 코스에 저장한다.
 */
@RestController
public class CourseRouteController {

    private final CourseRouteService courseRouteService;
    private final SavedCourseService savedCourseService;
    private final CurrentUserService currentUserService;

    public CourseRouteController(CourseRouteService courseRouteService,
                                 SavedCourseService savedCourseService,
                                 CurrentUserService currentUserService) {
        this.courseRouteService = courseRouteService;
        this.savedCourseService = savedCourseService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/api/course/{id}/route")
    public ResponseEntity<CourseRoute> route(@PathVariable Long id) {
        // 남의 코스 경로를 계산해 호출을 소모시키지 못하게 소유자만 허용한다
        var course = savedCourseService.find(id);
        if (course == null || !course.getUserId().equals(currentUserService.currentId())) {
            return ResponseEntity.notFound().build();
        }
        CourseRoute route = courseRouteService.route(id);
        return route == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(route);
    }
}
