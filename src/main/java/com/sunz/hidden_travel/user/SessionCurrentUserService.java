package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.repository.AppUserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 구현 전까지 쓰는 더미 사용자 구현.
 *
 * 중요: 세션마다 새 사용자를 만들지 않고 <b>고정 사용자 1명</b>("여행자")을 재사용한다.
 * 세션마다 새로 만들면 서버를 재시작할 때마다(세션 소멸) 신원이 바뀌어
 * 이전에 저장한 코스·후기가 전부 "남의 것"이 되어 내 코스 목록이 비어버린다.
 * 개발 중 데이터가 사라진 것처럼 보이는 문제를 막기 위해 고정 사용자를 쓴다.
 *
 * 세션에는 조회 결과를 캐시만 해둔다(매 요청 DB 조회 회피).
 *
 * 실제 인증 도입 시: 이 빈을 SecurityContext 에서 사용자를 꺼내는 구현으로 교체.
 * 그때는 사용자별로 자연히 분리되므로 이 클래스만 지우면 된다.
 */
@Service
public class SessionCurrentUserService implements CurrentUserService {

    private static final String SESSION_KEY = "currentUserId";

    /** 개발용 고정 사용자 닉네임 */
    private static final String DEV_NICKNAME = "여행자";

    private final AppUserRepository appUserRepository;

    public SessionCurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public AppUser current(HttpSession session) {
        // 1) 세션 캐시
        Object cached = session.getAttribute(SESSION_KEY);
        if (cached instanceof Long userId) {
            AppUser found = appUserRepository.findById(userId).orElse(null);
            if (found != null) {
                return found;
            }
            session.removeAttribute(SESSION_KEY); // DB 초기화 후 남은 옛 세션
        }

        // 2) 고정 사용자를 찾거나 없으면 한 번만 만든다
        AppUser user = appUserRepository.findFirstByNicknameOrderByIdAsc(DEV_NICKNAME)
                .orElseGet(() -> appUserRepository.save(new AppUser(DEV_NICKNAME)));
        session.setAttribute(SESSION_KEY, user.getId());
        return user;
    }
}
