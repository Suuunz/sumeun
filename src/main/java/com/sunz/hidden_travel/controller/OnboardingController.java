package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.mbti.TravelMbtiService;
import com.sunz.hidden_travel.mbti.TravelMbtiType;
import com.sunz.hidden_travel.repository.AppUserRepository;
import com.sunz.hidden_travel.user.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * 온보딩 = 여행 MBTI 검사.
 *
 * 가입 직후 흐름에 놓이지만 로그인 없이도 해볼 수 있다.
 * 비로그인이면 결과만 보여주고, 저장은 가입한 뒤에 하도록 안내한다.
 */
@Controller
public class OnboardingController {

    private final TravelMbtiService mbtiService;
    private final CurrentUserService currentUserService;
    private final AppUserRepository appUserRepository;

    public OnboardingController(TravelMbtiService mbtiService,
                                CurrentUserService currentUserService,
                                AppUserRepository appUserRepository) {
        this.mbtiService = mbtiService;
        this.currentUserService = currentUserService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/onboarding")
    public String onboarding(Model model) {
        model.addAttribute("questions", mbtiService.questions());
        model.addAttribute("loggedIn", currentUserService.currentId() != null);
        return "onboarding";
    }

    /** 답안 제출 → 유형 계산 + (로그인 상태면) 저장 */
    @PostMapping("/api/mbti/result")
    @ResponseBody
    @Transactional
    public Map<String, Object> result(@RequestBody Map<String, List<Integer>> body) {
        TravelMbtiType type = mbtiService.score(body == null ? null : body.get("answers"));
        if (type == null) {
            return Map.of("error", "답변이 올바르지 않아요. 다시 시도해 주세요.");
        }

        boolean saved = false;
        Long userId = currentUserService.currentId();
        if (userId != null) {
            AppUser user = appUserRepository.findById(userId).orElse(null);
            if (user != null) {
                user.setTravelMbti(type.getCode());
                saved = true;
            }
        }

        return Map.of(
                "code", type.getCode(),
                "label", type.getLabel(),
                "emoji", type.getEmoji(),
                "tagline", type.getTagline(),
                "style", type.getStyle(),
                "saved", saved);
    }
}
