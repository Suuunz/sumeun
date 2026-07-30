package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.repository.AppUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일로 사용자를 찾아 인증에 넘긴다.
 * 존재하지 않는 이메일과 틀린 비밀번호는 화면에서 동일한 메시지로 처리해
 * 가입 여부가 드러나지 않게 한다(SecurityConfig 의 실패 처리).
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return appUserRepository.findByEmail(email)
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("가입되지 않은 이메일입니다."));
    }
}
