package com.sunz.hidden_travel.repository;

import com.sunz.hidden_travel.domain.GoodPriceShop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoodPriceShopRepository extends JpaRepository<GoodPriceShop, Long> {

    List<GoodPriceShop> findBySigCd(String sigCd);

    boolean existsByNameAndAddr(String name, String addr);
}
