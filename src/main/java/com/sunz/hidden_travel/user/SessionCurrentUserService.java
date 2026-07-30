package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.repository.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 기반 더미 사용자 구현.
 * 로그인이 아직 구현되지 않았으므로(login.js 는 화면 이동만 한다)
 * 세션에 사용자 id 를 심어 "이 브라우저의 사용자"로 취급한다.
 *
 * 실제 인증 도입 시: 이 빈을 SecurityContext 에서 사용자를 꺼내는 구현으로 교체.
 */
@Service
public class SessionCurrentUserService implements CurrentUserService {

    private static final String SESSION_KEY = "currentUserId";
    private static final String DEFAULT_NICKNAME = "여행자";

    private final AppUserRepository appUserRepository;

    public SessionCurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public AppUser current(HttpSession session) {
        Object id = session.getAttribute(SESSION_KEY);
        if (id instanceof Long userId) {
            AppUser found = appUserRepository.findById(userId).orElse(null);
            if (found != null) {
                return found;
            }
            // DB 가 초기화된 뒤 옛 세션이 남은 경우 → 아래에서 새로 만든다
        }
        AppUser user = appUserRepository.save(new AppUser(DEFAULT_NICKNAME));
        session.setAttribute(SESSION_KEY, user.getId());
        return user;
    }
}
