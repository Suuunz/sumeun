package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;

/**
 * 현재 로그인한 사용자 조회. SecurityContext 를 감싸서
 * 컨트롤러가 인증 API 에 직접 의존하지 않게 한다.
 */
public interface CurrentUserService {

    /** 로그인한 사용자, 비로그인이면 null */
    AppUser current();

    /** 로그인한 사용자 id, 비로그인이면 null */
    Long currentId();
}
