package com.sunz.hidden_travel.controller;

import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.user.CurrentUserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에서 헤더가 로그인 상태를 알 수 있도록 현재 사용자를 모델에 넣어준다.
 * (비로그인 시 null — 템플릿에서 로그인/회원가입 링크를 보여준다)
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentUserService currentUserService;

    public GlobalModelAdvice(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        return currentUserService.current();
    }
}
