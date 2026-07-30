package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * 닉네임으로 사용자 1명 조회.
     * 정렬을 명시해야 동명이인이 있을 때 항상 같은 사람을 돌려준다
     * (정렬이 없으면 재시작마다 다른 사용자가 잡혀 내 코스가 비어 보일 수 있다).
     */
    Optional<AppUser> findFirstByNicknameOrderByIdAsc(String nickname);
}
