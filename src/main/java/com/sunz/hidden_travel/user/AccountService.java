package com.sunz.hidden_travel.user;

import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 / 프로필 수정.
 */
@Service
public class AccountService {

    /** 회원가입 실패 사유 — 컨트롤러가 화면 메시지로 바꾼다 */
    public static class SignupException extends RuntimeException {
        public SignupException(String message) {
            super(message);
        }
    }

    private static final int MIN_PASSWORD = 8;
    private static final int MAX_NICKNAME = 30;

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser signup(String email, String password, String confirmPassword, String nickname) {
        String mail = trim(email);
        String nick = trim(nickname);

        if (mail == null || !mail.contains("@")) {
            throw new SignupException("올바른 이메일 주소를 입력해 주세요.");
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            throw new SignupException("비밀번호는 " + MIN_PASSWORD + "자 이상이어야 합니다.");
        }
        if (!password.equals(confirmPassword)) {
            throw new SignupException("비밀번호가 서로 일치하지 않습니다.");
        }
        if (nick == null || nick.isBlank()) {
            throw new SignupException("닉네임을 입력해 주세요.");
        }
        if (nick.length() > MAX_NICKNAME) {
            throw new SignupException("닉네임은 " + MAX_NICKNAME + "자 이하로 입력해 주세요.");
        }
        if (appUserRepository.existsByEmail(mail)) {
            throw new SignupException("이미 가입된 이메일입니다.");
        }

        return appUserRepository.save(new AppUser(mail, passwordEncoder.encode(password), nick));
    }

    /** 프로필 수정 — 닉네임/소개는 항상, 사진은 새로 올렸을 때만 교체 */
    @Transactional
    public AppUser updateProfile(Long userId, String nickname, String bio, String newImagePath) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new SignupException("사용자를 찾을 수 없습니다."));

        String nick = trim(nickname);
        if (nick == null || nick.isBlank()) {
            throw new SignupException("닉네임을 입력해 주세요.");
        }
        if (nick.length() > MAX_NICKNAME) {
            throw new SignupException("닉네임은 " + MAX_NICKNAME + "자 이하로 입력해 주세요.");
        }

        user.setNickname(nick);
        user.setBio(trim(bio));
        if (newImagePath != null) {
            user.setProfileImage(newImagePath);
        }
        return user;
    }

    /** 비밀번호 변경 — 현재 비밀번호를 확인한다 */
    @Transactional
    public void changePassword(Long userId, String current, String next, String confirm) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new SignupException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(current, user.getPassword())) {
            throw new SignupException("현재 비밀번호가 올바르지 않습니다.");
        }
        if (next == null || next.length() < MIN_PASSWORD) {
            throw new SignupException("새 비밀번호는 " + MIN_PASSWORD + "자 이상이어야 합니다.");
        }
        if (!next.equals(confirm)) {
            throw new SignupException("새 비밀번호가 서로 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(next));
    }

    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
