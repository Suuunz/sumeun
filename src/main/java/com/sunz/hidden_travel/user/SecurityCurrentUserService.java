package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SecurityContext 기반 구현. 인증된 사용자의 id 로 실제 엔티티를 읽어온다.
 * (닉네임·프로필 사진이 수정돼도 항상 최신값을 보게 된다)
 */
@Service
public class SecurityCurrentUserService implements CurrentUserService {

    private final AppUserRepository appUserRepository;

    public SecurityCurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AppUser current() {
        Long id = currentId();
        return id == null ? null : appUserRepository.findById(id).orElse(null);
    }

    @Override
    public Long currentId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        // 익명 사용자(AnonymousAuthenticationToken)의 principal 은 문자열이다
        return (auth.getPrincipal() instanceof AppUserDetails d) ? d.getId() : null;
    }
}
