package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.ApiCallUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiCallUsageRepository extends JpaRepository<ApiCallUsage, String> {
}
