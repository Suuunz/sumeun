package com.sunz.hidden_travel.init;

import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.repository.RegionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * static/geo/sig.json(TopoJSON)을 읽어 전국 시군구 Region 테이블을 시딩한다.
 * - 이미 존재하는 SIG_CD 는 건너뛰는 upsert(=insert-if-absent) 방식.
 * - 대표 좌표(lat,lng)는 시군구 폴리곤의 면적 가중 중심점(centroid)으로 계산.
 * - province 는 SIG_CD 앞 2자리(시도 코드)로 매핑.
 *
 * 이 Region(중심좌표 + 폴리곤)이 이후 TourAPI/착한가격업소 데이터를
 * 좌표→SIG_CD 로 역매핑하는 기준이 된다.
 */
@Component
public class RegionSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RegionSeeder.class);

    /** 시도 코드(SIG_CD 앞 2자리) → 시도명 */
    private static final Map<String, String> SIDO = Map.ofEntries(
            Map.entry("11", "서울특별시"), Map.entry("26", "부산광역시"),
            Map.entry("27", "대구광역시"), Map.entry("28", "인천광역시"),
            Map.entry("29", "광주광역시"), Map.entry("30", "대전광역시"),
            Map.entry("31", "울산광역시"), Map.entry("36", "세종특별자치시"),
            Map.entry("41", "경기도"), Map.entry("42", "강원특별자치도"),
            Map.entry("43", "충청북도"), Map.entry("44", "충청남도"),
            Map.entry("45", "전북특별자치도"), Map.entry("46", "전라남도"),
            Map.entry("47", "경상북도"), Map.entry("48", "경상남도"),
            Map.entry("50", "제주특별자치도")
    );

    private final RegionRepository regionRepository;

    public RegionSeeder(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long already = regionRepository.count();

        JsonNode root;
        try (InputStream in = new ClassPathResource("static/geo/sig.json").getInputStream()) {
            root = new ObjectMapper().readTree(in);
        } catch (Exception e) {
            log.error("[RegionSeeder] sig.json 로드 실패 — 시딩 건너뜀: {}", e.getMessage());
            return;
        }

        // 1) 양자화 transform
        JsonNode tr = root.get("transform");
        double sx = tr.get("scale").get(0).doubleValue();
        double sy = tr.get("scale").get(1).doubleValue();
        double tx = tr.get("translate").get(0).doubleValue();
        double ty = tr.get("translate").get(1).doubleValue();

        // 2) arcs 디코딩(델타 누적 → [lng,lat])
        JsonNode arcsNode = root.get("arcs");
        double[][][] arcs = new double[arcsNode.size()][][];
        for (int a = 0; a < arcsNode.size(); a++) {
            JsonNode arc = arcsNode.get(a);
            double[][] pts = new double[arc.size()][2];
            long qx = 0, qy = 0;
            for (int i = 0; i < arc.size(); i++) {
                qx += arc.get(i).get(0).intValue();
                qy += arc.get(i).get(1).intValue();
                pts[i][0] = qx * sx + tx; // lng
                pts[i][1] = qy * sy + ty; // lat
            }
            arcs[a] = pts;
        }

        // 3) geometries 순회 → centroid 계산 → Region upsert
        JsonNode objects = root.get("objects");
        String firstObjName = objects.propertyNames().iterator().next();
        JsonNode geometries = objects.get(firstObjName).get("geometries");

        List<Region> toInsert = new ArrayList<>();
        int skipped = 0;
        for (int g = 0; g < geometries.size(); g++) {
            JsonNode geom = geometries.get(g);
            JsonNode props = geom.get("properties");
            String sigCd = props.get("SIG_CD").asString();
            String name = props.get("SIG_KOR_NM").asString();

            if (regionRepository.existsById(sigCd)) {
                skipped++;
                continue;
            }

            double[] centroid = centroid(geom, arcs); // [lng, lat]

            Region region = new Region();
            region.setSigCd(sigCd);
            region.setName(name);
            region.setProvince(SIDO.getOrDefault(sigCd.substring(0, 2), ""));
            region.setLng(centroid[0]);
            region.setLat(centroid[1]);
            toInsert.add(region);
        }

        regionRepository.saveAll(toInsert);

        long total = regionRepository.count();
        log.info("[RegionSeeder] 완료 — 파일 {}개 / 신규 {}개 / 스킵(기존) {}개 / DB 총 {}개 (기존 {}개)",
                geometries.size(), toInsert.size(), skipped, total, already);

        // 샘플 출력
        for (String cd : new String[]{"47170", "42770", "11110", "50110", "47940", "48820"}) {
            regionRepository.findById(cd).ifPresent(r ->
                    log.info("[RegionSeeder]   샘플 {} {} ({}) → lat={}, lng={}",
                            r.getSigCd(), r.getName(), r.getProvince(),
                            round6(r.getLat()), round6(r.getLng())));
        }
    }

    /* ---------- centroid (면적 가중, 폴리곤 외곽 링 기준) ---------- */
    private double[] centroid(JsonNode geom, double[][][] arcs) {
        String type = geom.get("type").asString();
        JsonNode arcsField = geom.get("arcs");
        List<List<double[]>> outerRings = new ArrayList<>();

        if ("Polygon".equals(type)) {
            outerRings.add(ring(arcsField.get(0), arcs)); // 첫 링 = 외곽
        } else if ("MultiPolygon".equals(type)) {
            for (int p = 0; p < arcsField.size(); p++) {
                outerRings.add(ring(arcsField.get(p).get(0), arcs));
            }
        }

        double totA = 0, sumX = 0, sumY = 0;
        for (List<double[]> r : outerRings) {
            double[] ca = ringCentroidArea(r); // [cx, cy, area]
            double w = ca[2];
            totA += w;
            sumX += ca[0] * w;
            sumY += ca[1] * w;
        }
        if (totA > 0) {
            return new double[]{sumX / totA, sumY / totA};
        }
        // 면적이 0에 수렴 → 전체 점 평균으로 폴백
        double ax = 0, ay = 0;
        int n = 0;
        for (List<double[]> r : outerRings) {
            for (double[] pt : r) { ax += pt[0]; ay += pt[1]; n++; }
        }
        return n > 0 ? new double[]{ax / n, ay / n} : new double[]{0, 0};
    }

    /** 링(arc 인덱스 배열)을 실제 좌표 시퀀스로 스티칭 */
    private List<double[]> ring(JsonNode ringArcs, double[][][] arcs) {
        List<double[]> coords = new ArrayList<>();
        for (int k = 0; k < ringArcs.size(); k++) {
            int idx = ringArcs.get(k).intValue();
            boolean reversed = idx < 0;
            double[][] arc = reversed ? arcs[~idx] : arcs[idx];
            int len = arc.length;
            for (int i = 0; i < len; i++) {
                if (i == 0 && !coords.isEmpty()) continue; // 이웃 arc와 공유하는 끝점 중복 제거
                coords.add(arc[reversed ? (len - 1 - i) : i]);
            }
        }
        return coords;
    }

    /** 단일 링의 [cx, cy, |area|] (shoelace) */
    private double[] ringCentroidArea(List<double[]> r) {
        int n = r.size();
        double a = 0, cx = 0, cy = 0;
        for (int i = 0; i < n; i++) {
            double[] p0 = r.get(i);
            double[] p1 = r.get((i + 1) % n);
            double cross = p0[0] * p1[1] - p1[0] * p0[1];
            a += cross;
            cx += (p0[0] + p1[0]) * cross;
            cy += (p0[1] + p1[1]) * cross;
        }
        a *= 0.5;
        if (Math.abs(a) < 1e-12) {
            double sx = 0, sy = 0;
            for (double[] p : r) { sx += p[0]; sy += p[1]; }
            return new double[]{sx / n, sy / n, 0};
        }
        return new double[]{cx / (6 * a), cy / (6 * a), Math.abs(a)};
    }

    private double round6(Double v) {
        return v == null ? 0 : Math.round(v * 1_000_000d) / 1_000_000d;
    }
}
