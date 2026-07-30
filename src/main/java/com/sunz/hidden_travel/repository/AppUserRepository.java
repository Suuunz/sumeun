package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findFirstByNickname(String nickname);
}
