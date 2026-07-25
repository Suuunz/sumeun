package com.sunz.hidden_travel.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 화면 라우팅 전용 컨트롤러.
 * 현재는 퍼블리싱 단계로, 각 경로에 대응하는 뷰 이름만 반환한다.
 * (DB / JPA / Security / 외부 API 연동 없음)
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String login() {
        return "login";
    }

    @GetMapping("/onboarding")
    public String onboarding() {
        return "onboarding";
    }

    @GetMapping("/map")
    public String map() {
        return "map";
    }

    @GetMapping("/region")
    public String regionDetail() {
        return "region-detail";
    }

    @GetMapping("/course")
    public String course() {
        return "course";
    }
}
