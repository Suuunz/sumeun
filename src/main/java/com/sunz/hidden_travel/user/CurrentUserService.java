package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;
import jakarta.servlet.http.HttpSession;

/**
 * 현재 사용자 조회. 지금은 세션 기반 더미 구현이지만,
 * 실제 인증(Spring Security) 도입 시 이 인터페이스의 구현만 교체하면 된다.
 * (RecommendService 와 동일한 교체 전략)
 */
public interface CurrentUserService {

    /** 현재 사용자. 없으면 만들어서라도 반환한다(null 없음). */
    AppUser current(HttpSession session);
}
