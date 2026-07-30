package com.sunz.hidden_travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 서비스 사용자. 코스/후기의 작성자.
 * 현재 로그인은 구현 전이라 세션당 더미 사용자를 만들어 쓴다
 * ({@link com.sunz.hidden_travel.user.CurrentUserService}).
 * 실제 인증 도입 시 이 엔티티에 이메일·비밀번호 필드를 추가하면 된다.
 */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 화면에 노출되는 이름 */
    @Column(nullable = false)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AppUser(String nickname) {
        this.nickname = nickname;
    }
}
