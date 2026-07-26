package com.sunz.hidden_travel.tour;

import com.sunz.hidden_travel.domain.Attraction;
import com.sunz.hidden_travel.domain.CoursePoint;
import com.sunz.hidden_travel.domain.FoodPlace;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.domain.TravelCourse;
import com.sunz.hidden_travel.geo.SigGeometryService;
import com.sunz.hidden_travel.repository.AttractionRepository;
import com.sunz.hidden_travel.repository.FoodPlaceRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import com.sunz.hidden_travel.repository.TravelCourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TourAPI 적재 배치. 웹 요청과 분리되어 관리자 엔드포인트로만 호출된다.
 *
 * 좌표 기반 SIG_CD 판정:
 *  - 단일 지역(syncRegion): Region 중심좌표 + 반경으로 locationBasedList 호출 후,
 *    각 항목을 그 지역 폴리곤과 point-in-polygon 으로 확인해 적재(인접 지역 항목 배제).
 *  - 시도(syncSido): areaBasedList 로 시도 전체를 받아 각 항목 좌표로 SIG_CD 판정.
 * 중복은 sourceContentId(=TourAPI contentId) 기준으로 방지한다.
 */
@Service
public class TourSyncService {

    private static final Logger log = LoggerFactory.getLogger(TourSyncService.class);

    private static final int CT_ATTRACTION = 12;
    private static final int CT_FOOD = 39;
    private static final int CT_COURSE = 25;
    private static final int[] CONTENT_TYPES = {CT_ATTRACTION, CT_FOOD, CT_COURSE};

    private static final int NUM_OF_ROWS = 100;
    private static final int MAX_PAGES = 30;          // 지역/시도당 안전 상한
    private static final int SINGLE_RADIUS = 20000;   // locationBased 최대 반경(m)
    private static final long CALL_DELAY_MS = 150;    // rate limit 회피

    /** 시도 코드(SIG_CD 앞 2자리) → TourAPI areaCode */
    private static final Map<String, Integer> SIDO_TO_AREA = Map.ofEntries(
            Map.entry("11", 1), Map.entry("28", 2), Map.entry("30", 3), Map.entry("27", 4),
            Map.entry("29", 5), Map.entry("26", 6), Map.entry("31", 7), Map.entry("36", 8),
            Map.entry("41", 31), Map.entry("42", 32), Map.entry("43", 33), Map.entry("44", 34),
            Map.entry("47", 35), Map.entry("48", 36), Map.entry("45", 37), Map.entry("46", 38),
            Map.entry("50", 39)
    );

    private final TourApiClient client;
    private final SigGeometryService geometry;
    private final RegionRepository regionRepository;
    private final AttractionRepository attractionRepository;
    private final FoodPlaceRepository foodPlaceRepository;
    private final TravelCourseRepository travelCourseRepository;

    public TourSyncService(TourApiClient client, SigGeometryService geometry,
                           RegionRepository regionRepository,
                           AttractionRepository attractionRepository,
                           FoodPlaceRepository foodPlaceRepository,
                           TravelCourseRepository travelCourseRepository) {
        this.client = client;
        this.geometry = geometry;
        this.regionRepository = regionRepository;
        this.attractionRepository = attractionRepository;
        this.foodPlaceRepository = foodPlaceRepository;
        this.travelCourseRepository = travelCourseRepository;
    }

    /* =========================================================
       단일 지역 (검증용) — 좌표 반경 + point-in-polygon
       ========================================================= */
    public Map<String, Object> syncRegion(String sigCd) {
        Region region = regionRepository.findById(sigCd).orElse(null);
        if (region == null || region.getLat() == null || region.getLng() == null) {
            return Map.of("error", "region 없음 또는 좌표 없음: " + sigCd);
        }
        Counter c = new Counter();
        for (int type : CONTENT_TYPES) {
            for (int page = 1; page <= MAX_PAGES; page++) {
                TourApiClient.TourPage p = client.locationBasedList(
                        region.getLng(), region.getLat(), SINGLE_RADIUS, type, page, NUM_OF_ROWS);
                if (p.items().isEmpty()) break;
                for (JsonNode item : p.items()) {
                    double[] xy = coord(item);
                    if (xy == null) { c.unmapped++; continue; }
                    // 반경 안이어도 실제로 이 지역 폴리곤에 속하는지 확인
                    if (!geometry.isInSigCd(sigCd, xy[0], xy[1])) { c.outside++; continue; }
                    upsert(type, item, sigCd, xy, c);
                }
                if (p.items().size() < NUM_OF_ROWS) break;
                sleep();
            }
        }
        Map<String, Object> result = summary(sigCd, c);
        log.info("[TourSync] syncRegion {} → {}", sigCd, result);
        return result;
    }

    /* =========================================================
       시도 단위 (전체 배치) — areaBasedList + 좌표 판정
       ========================================================= */
    public Map<String, Object> syncSido(int areaCode) {
        String sidoPrefix = sidoPrefixOf(areaCode);
        Counter c = new Counter();
        for (int type : CONTENT_TYPES) {
            for (int page = 1; page <= MAX_PAGES; page++) {
                TourApiClient.TourPage p = client.areaBasedList(areaCode, type, page, NUM_OF_ROWS);
                if (p.items().isEmpty()) break;
                for (JsonNode item : p.items()) {
                    double[] xy = coord(item);
                    if (xy == null) { c.unmapped++; continue; }
                    String sigCd = geometry.resolveSigCd(xy[0], xy[1], sidoPrefix)
                            .orElseGet(() -> geometry.resolveSigCd(xy[0], xy[1]).orElse(null));
                    if (sigCd == null) {
                        c.outside++;
                        log.debug("[TourSync] 좌표→SIG_CD 실패: {} ({},{})", title(item), xy[0], xy[1]);
                        continue;
                    }
                    upsert(type, item, sigCd, xy, c);
                }
                if (p.items().size() < NUM_OF_ROWS) break;
                sleep();
            }
        }
        Map<String, Object> result = summary("area:" + areaCode, c);
        log.info("[TourSync] syncSido {} → {}", areaCode, result);
        return result;
    }

    /** 전국 배치 (17개 시도 순회) */
    public Map<String, Object> syncAll() {
        Map<String, Object> all = new LinkedHashMap<>();
        for (Integer areaCode : new java.util.TreeSet<>(SIDO_TO_AREA.values())) {
            all.put("area:" + areaCode, syncSido(areaCode));
        }
        return all;
    }

    /* =========================================================
       upsert
       ========================================================= */
    private void upsert(int type, JsonNode item, String sigCd, double[] xy, Counter c) {
        String cid = text(item, "contentid");
        if (cid == null || cid.isBlank()) { c.unmapped++; return; }
        switch (type) {
            case CT_ATTRACTION -> {
                if (attractionRepository.existsBySourceContentId(cid)) { c.skipped++; return; }
                Attraction a = new Attraction();
                a.setSigCd(sigCd);
                a.setName(text(item, "title"));
                a.setType("관광지");
                a.setAddr(addr(item));
                a.setLng(xy[0]);
                a.setLat(xy[1]);
                a.setSourceContentId(cid);
                a.setImage(firstNonBlank(text(item, "firstimage"), text(item, "firstimage2")));
                attractionRepository.save(a);
                c.attractions++;
            }
            case CT_FOOD -> {
                if (foodPlaceRepository.existsBySourceContentId(cid)) { c.skipped++; return; }
                FoodPlace f = new FoodPlace();
                f.setSigCd(sigCd);
                f.setName(text(item, "title"));
                f.setCategory(text(item, "cat3"));
                f.setAddr(addr(item));
                f.setLng(xy[0]);
                f.setLat(xy[1]);
                f.setSourceContentId(cid);
                foodPlaceRepository.save(f);
                c.foodPlaces++;
            }
            case CT_COURSE -> {
                if (travelCourseRepository.existsBySourceContentId(cid)) { c.skipped++; return; }
                TravelCourse tc = new TravelCourse();
                tc.setSigCd(sigCd);
                tc.setTitle(text(item, "title"));
                tc.setTheme(text(item, "cat2"));
                tc.setSourceContentId(cid);
                fillCoursePoints(tc, cid);
                travelCourseRepository.save(tc);
                c.travelCourses++;
            }
            default -> { }
        }
    }

    /** 여행코스 경유지(detailInfo2) — best-effort */
    private void fillCoursePoints(TravelCourse tc, String cid) {
        try {
            List<JsonNode> sub = client.detailInfo(cid, CT_COURSE);
            List<CoursePoint> points = new ArrayList<>();
            int order = 1;
            for (JsonNode s : sub) {
                CoursePoint cp = new CoursePoint();
                cp.setOrder(order++);
                cp.setName(text(s, "subname"));
                cp.setDescription(text(s, "subdetailoverview"));
                points.add(cp);
            }
            tc.setPoints(points);
        } catch (Exception e) {
            log.debug("[TourSync] 코스 경유지 조회 실패 cid={}: {}", cid, e.getMessage());
        }
    }

    /* ---------- 유틸 ---------- */
    private double[] coord(JsonNode item) {
        Double x = parse(text(item, "mapx"));
        Double y = parse(text(item, "mapy"));
        if (x == null || y == null || x == 0.0 || y == 0.0) return null;
        return new double[]{x, y};
    }

    private String addr(JsonNode item) {
        String a1 = text(item, "addr1");
        String a2 = text(item, "addr2");
        if (a1 == null) return a2;
        return (a2 == null || a2.isBlank()) ? a1 : a1 + " " + a2;
    }

    private String text(JsonNode item, String field) {
        JsonNode n = item.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asString();
    }

    private String title(JsonNode item) {
        String t = text(item, "title");
        return t == null ? "?" : t;
    }

    private Double parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : (b != null && !b.isBlank() ? b : null);
    }

    private String sidoPrefixOf(int areaCode) {
        return SIDO_TO_AREA.entrySet().stream()
                .filter(e -> e.getValue() == areaCode)
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private void sleep() {
        try {
            Thread.sleep(CALL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> summary(String scope, Counter c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", scope);
        m.put("attractions", c.attractions);
        m.put("foodPlaces", c.foodPlaces);
        m.put("travelCourses", c.travelCourses);
        m.put("skipped(existing)", c.skipped);
        m.put("outsidePolygon", c.outside);
        m.put("noCoord", c.unmapped);
        return m;
    }

    private static final class Counter {
        int attractions, foodPlaces, travelCourses, skipped, outside, unmapped;
    }
}
