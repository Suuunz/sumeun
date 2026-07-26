package com.sunz.hidden_travel.init;

import com.opencsv.CSVReader;
import com.sunz.hidden_travel.domain.Specialty;
import com.sunz.hidden_travel.repository.SpecialtyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 지역 특산물 시드(CSV) 적재. (옵션 A — 표준 공공 API가 없어 시드로 처리)
 * 컬럼: sigCd, name, season. UTF-8. 이미 존재하는 (sigCd,name) 은 skip.
 * RegionSeeder 이후 실행되도록 Order 를 뒤로 둔다.
 */
@Component
@Order(20)
public class SpecialtySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SpecialtySeeder.class);
    private static final String CSV = "specialties-seed.csv";

    private final SpecialtyRepository specialtyRepository;

    public SpecialtySeeder(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<String[]> rows;
        try (InputStream in = new ClassPathResource(CSV).getInputStream();
             Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
             CSVReader csv = new CSVReader(reader)) {
            rows = csv.readAll();
        } catch (Exception e) {
            log.error("[SpecialtySeeder] {} 로드 실패: {}", CSV, e.getMessage());
            return;
        }

        int inserted = 0, skipped = 0;
        for (int i = 1; i < rows.size(); i++) { // 헤더 skip
            String[] r = rows.get(i);
            if (r.length < 2) continue;
            String sigCd = trim(r[0]);
            String name = trim(r[1]);
            String season = r.length > 2 ? trim(r[2]) : null;
            if (sigCd == null || name == null) continue;

            if (specialtyRepository.existsBySigCdAndName(sigCd, name)) {
                skipped++;
                continue;
            }
            Specialty s = new Specialty();
            s.setSigCd(sigCd);
            s.setName(name);
            s.setSeason(season);
            specialtyRepository.save(s);
            inserted++;
        }
        log.info("[SpecialtySeeder] 완료 — 신규 {}개 / 스킵(기존) {}개 / DB 총 {}개",
                inserted, skipped, specialtyRepository.count());
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
