package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    List<Specialty> findBySigCd(String sigCd);
}
