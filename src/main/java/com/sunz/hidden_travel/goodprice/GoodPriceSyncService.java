package com.sunz.hidden_travel.goodprice;

import com.opencsv.CSVReader;
import com.sunz.hidden_travel.domain.GoodPriceMenu;
import com.sunz.hidden_travel.domain.GoodPriceShop;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.repository.GoodPriceShopRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 행정안전부 착한가격업소 CSV 적재.
 * 좌표가 없으므로 "시도명 + 시군명 → SIG_CD" 매핑으로 적재한다(Region 테이블 재사용).
 * 배치는 관리자 엔드포인트로만 실행되고, 사용자 화면은 DB만 읽는다.
 */
@Service
public class GoodPriceSyncService {

    private static final Logger log = LoggerFactory.getLogger(GoodPriceSyncService.class);

    private static final String CSV = "good-price-shops.csv";
    // 컬럼: 0 시도,1 시군,2 업종,3 업소명,4 연락처,5 주소,6 메뉴1,7 가격1,8 메뉴2,9 가격2,10 메뉴3,11 가격3,12 메뉴4,13 가격4
    private static final int C_SIDO = 0, C_SIGUN = 1, C_CATEGORY = 2, C_NAME = 3, C_PHONE = 4, C_ADDR = 5;
    private static final int[][] MENU_PAIRS = {{6, 7}, {8, 9}, {10, 11}, {12, 13}};

    /** 시도명 → 시도 2자리 코드 (4-B SIDO 매핑의 역방향) */
    private static final Map<String, String> SIDO_NAME_TO_CODE = Map.ofEntries(
            Map.entry("서울특별시", "11"), Map.entry("부산광역시", "26"), Map.entry("대구광역시", "27"),
            Map.entry("인천광역시", "28"), Map.entry("광주광역시", "29"), Map.entry("대전광역시", "30"),
            Map.entry("울산광역시", "31"), Map.entry("세종특별자치시", "36"), Map.entry("경기도", "41"),
            Map.entry("강원특별자치도", "42"), Map.entry("충청북도", "43"), Map.entry("충청남도", "44"),
            Map.entry("전북특별자치도", "45"), Map.entry("전라남도", "46"), Map.entry("경상북도", "47"),
            Map.entry("경상남도", "48"), Map.entry("제주특별자치도", "50")
    );

    private final RegionRepository regionRepository;
    private final GoodPriceShopRepository shopRepository;

    /** 시도 2자리 코드 → 해당 시도 Region 목록(정규화 이름 포함) */
    private Map<String, List<RegionRef>> bySido;
    /** 전국에서 이름이 유일한 시군구(정규화) → SIG_CD (행정구역 개편으로 시도가 바뀐 경우 보정용) */
    private Map<String, String> uniqueNameToSig;

    public GoodPriceSyncService(RegionRepository regionRepository, GoodPriceShopRepository shopRepository) {
        this.regionRepository = regionRepository;
        this.shopRepository = shopRepository;
    }

    private record RegionRef(String sigCd, String nameNorm) {}

    private void ensureRegionIndex() {
        if (bySido != null) return;
        bySido = new LinkedHashMap<>();
        Map<String, Integer> nameCount = new LinkedHashMap<>();
        Map<String, String> nameToSig = new LinkedHashMap<>();
        for (Region r : regionRepository.findAll()) {
            if (r.getSigCd() == null || r.getSigCd().length() < 2) continue;
            String nm = norm(r.getName());
            bySido.computeIfAbsent(r.getSigCd().substring(0, 2), k -> new ArrayList<>())
                    .add(new RegionRef(r.getSigCd(), nm));
            nameCount.merge(nm, 1, Integer::sum);
            nameToSig.put(nm, r.getSigCd());
        }
        uniqueNameToSig = new LinkedHashMap<>();
        nameCount.forEach((nm, cnt) -> {
            if (cnt == 1) uniqueNameToSig.put(nm, nameToSig.get(nm));
        });
    }

    /**
     * @param sidoCode null 이면 전체, 아니면 해당 시도 2자리 코드만 (예: 경북 "47").
     *                 (URL 한글 인코딩 문제를 피하려 이름 대신 코드로 필터)
     */
    public Map<String, Object> sync(String sidoCode) {
        ensureRegionIndex();

        List<String[]> rows = readCsv();
        if (rows.isEmpty()) {
            return Map.of("error", "CSV 읽기 실패 또는 빈 파일");
        }

        int total = 0, inserted = 0, skipped = 0, unmapped = 0, priceFail = 0;
        List<String> unmappedSamples = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) { // 헤더 skip
            String[] row = rows.get(i);
            if (row.length <= C_ADDR) continue;
            String sido = trim(row[C_SIDO]);
            String rowCode = SIDO_NAME_TO_CODE.get(sido);
            if (sidoCode != null && !sidoCode.equals(rowCode)) continue;
            total++;

            String sigun = trim(row[C_SIGUN]);
            String name = trim(row[C_NAME]);
            String addr = trim(row[C_ADDR]);

            String sigCd = resolveSigCd(sido, sigun, addr);
            if (sigCd == null) {
                unmapped++;
                if (unmappedSamples.size() < 15) {
                    unmappedSamples.add(name + " | " + sido + " " + sigun + " | " + addr);
                }
                continue;
            }

            if (name != null && addr != null && shopRepository.existsByNameAndAddr(name, addr)) {
                skipped++;
                continue;
            }

            // 메뉴/가격 4쌍 수집
            List<GoodPriceMenu> menus = new ArrayList<>();
            for (int[] pair : MENU_PAIRS) {
                if (row.length <= pair[1]) break;
                String mn = trim(row[pair[0]]);
                if (mn == null || mn.isBlank()) continue;
                Integer pr = parsePrice(row[pair[1]]);
                if (pr == null && trim(row[pair[1]]) != null) priceFail++;
                menus.add(new GoodPriceMenu(mn, pr));
            }

            GoodPriceShop shop = new GoodPriceShop();
            shop.setSigCd(sigCd);
            shop.setName(name);
            shop.setCategory(trim(row[C_CATEGORY]));
            shop.setPhone(trim(row[C_PHONE]));
            shop.setAddr(addr);
            shop.setSource("행정안전부");
            if (!menus.isEmpty()) {
                shop.setMenu(menus.get(0).getMenu());
                shop.setPrice(menus.get(0).getPrice());
                shop.setMenus(menus);
            }
            shopRepository.save(shop);
            inserted++;
        }

        int mapped = total - unmapped;
        double rate = total == 0 ? 0 : (mapped * 100.0 / total);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scope", sidoCode == null ? "전국" : "sido:" + sidoCode);
        result.put("total", total);
        result.put("inserted", inserted);
        result.put("skipped(existing)", skipped);
        result.put("unmapped", unmapped);
        result.put("mappingRate", String.format("%.1f%%", rate));
        result.put("priceParseFail", priceFail);
        log.info("[GoodPriceSync] {}", result);
        if (!unmappedSamples.isEmpty()) {
            log.warn("[GoodPriceSync] 미매핑 샘플({}건 중 일부):", unmapped);
            unmappedSamples.forEach(s -> log.warn("  - {}", s));
        }
        return result;
    }

    /* ---------- 시도+시군 → SIG_CD ---------- */
    private String resolveSigCd(String sido, String sigun, String addr) {
        String code = SIDO_NAME_TO_CODE.get(sido);
        if (code == null) return null;
        List<RegionRef> regions = bySido.get(code);
        if (regions == null || regions.isEmpty()) return null;

        String sgNorm = norm(sigun);
        String addrNorm = norm(addr);

        if (sgNorm != null && !sgNorm.isEmpty()) {
            // 1) 정확 일치 (안동시, 경주시, 종로구 …)
            for (RegionRef r : regions) {
                if (r.nameNorm().equals(sgNorm)) return r.sigCd();
            }
            // 2) 접두 일치 (통합시: "포항시" → 포항시남구/포항시북구)
            List<RegionRef> cand = new ArrayList<>();
            for (RegionRef r : regions) {
                if (r.nameNorm().startsWith(sgNorm)) cand.add(r);
            }
            if (cand.size() == 1) return cand.get(0).sigCd();
            if (cand.size() > 1 && addrNorm != null) {
                for (RegionRef r : cand) {
                    String guSuffix = r.nameNorm().substring(sgNorm.length()); // 예: "남구"
                    if (!guSuffix.isEmpty() && addrNorm.contains(guSuffix)) return r.sigCd();
                }
            }
        }

        // 3) 주소 기반 (시군 빈값/표기 상이): 주소에 지역명이 통째로 포함되면 매칭(최장)
        if (addrNorm != null) {
            RegionRef best = null;
            for (RegionRef r : regions) {
                if (addrNorm.contains(r.nameNorm())
                        && (best == null || r.nameNorm().length() > best.nameNorm().length())) {
                    best = r;
                }
            }
            if (best != null) return best.sigCd();
        }

        // 4) 시군 빈값이고 시도에 지역이 하나뿐(세종)
        if ((sgNorm == null || sgNorm.isEmpty()) && regions.size() == 1) {
            return regions.get(0).sigCd();
        }

        // 5) 행정구역 개편 보정: 시군명이 전국에서 유일하면 시도 불일치여도 매핑
        //    (예: 군위군 — CSV는 대구(2023 편입), 우리 경계는 경북 47720)
        if (sgNorm != null && uniqueNameToSig.containsKey(sgNorm)) {
            return uniqueNameToSig.get(sgNorm);
        }
        return null;
    }

    /* ---------- CSV 읽기 (CP949 우선, 깨지면 UTF-8) ---------- */
    private List<String[]> readCsv() {
        List<String[]> rows = read(Charset.forName("MS949")); // CP949
        if (!rows.isEmpty() && "시도".equals(trim(rows.get(0)[0]))) {
            log.info("[GoodPriceSync] CSV 인코딩=CP949(MS949), 헤더 정상, 총 {}행", rows.size());
            return rows;
        }
        log.warn("[GoodPriceSync] CP949 헤더 비정상 → UTF-8 재시도");
        rows = read(Charset.forName("UTF-8"));
        return rows;
    }

    private List<String[]> read(Charset charset) {
        try (InputStream in = new ClassPathResource(CSV).getInputStream();
             Reader reader = new InputStreamReader(in, charset);
             CSVReader csv = new CSVReader(reader)) {
            return csv.readAll();
        } catch (Exception e) {
            log.error("[GoodPriceSync] CSV 읽기 실패({}): {}", charset, e.getMessage());
            return List.of();
        }
    }

    /* ---------- 유틸 ---------- */
    private String norm(String s) {
        return s == null ? null : s.replaceAll("\\s+", "");
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Integer parsePrice(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
