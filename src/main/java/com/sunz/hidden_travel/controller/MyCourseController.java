package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.service.ReviewService;
import com.sunz.hidden_travel.user.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 내 코스 목록 화면. 저장한 코스마다 후기 쓰기/보기로 이어진다.
 */
@Controller
public class MyCourseController {

    private final ReviewService reviewService;
    private final CurrentUserService currentUserService;

    public MyCourseController(ReviewService reviewService, CurrentUserService currentUserService) {
        this.reviewService = reviewService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/my/courses")
    public String myCourses(HttpSession session, Model model) {
        Long userId = currentUserService.current(session).getId();
        model.addAttribute("courses", reviewService.myCourseCards(userId));
        return "my-courses";
    }
}
