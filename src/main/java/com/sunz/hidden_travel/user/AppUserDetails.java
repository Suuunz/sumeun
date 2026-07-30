package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 인증 주체. {@link AppUser} 의 id 를 들고 있어 컨트롤러에서 바로 작성자 식별에 쓴다.
 * (로그인 아이디는 이메일이지만, 데이터는 항상 id 로 연결한다 — 이메일이 바뀌어도 안전)
 */
public class AppUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String nickname;

    public AppUserDetails(AppUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.nickname = user.getNickname();
    }

    public Long getId() {
        return id;
    }

    public String getNickname() {
        return nickname;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    /** Spring Security 의 "username" = 우리 서비스의 이메일 */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
