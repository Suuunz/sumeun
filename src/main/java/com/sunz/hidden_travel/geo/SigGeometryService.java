package com.sunz.hidden_travel.geo;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * sig.json(TopoJSON) 폴리곤을 로드해 좌표 → SIG_CD 판정을 제공한다.
 * TourAPI 응답의 (mapx, mapy) 를 이 폴리곤과 point-in-polygon 으로 대조해
 * 우리 SIG_CD(행정표준코드)로 역매핑한다.
 */
@Component
public class SigGeometryService {

    private static final Logger log = LoggerFactory.getLogger(SigGeometryService.class);

    /** 폴리곤 1개(외곽 링 + 구멍 + 경계상자) */
    private static final class Poly {
        final double[][] outer;
        final List<double[][]> holes;
        final double minX, minY, maxX, maxY;

        Poly(double[][] outer, List<double[][]> holes) {
            this.outer = outer;
            this.holes = holes;
            double mnX = Double.MAX_VALUE, mnY = Double.MAX_VALUE, mxX = -Double.MAX_VALUE, mxY = -Double.MAX_VALUE;
            for (double[] p : outer) {
                mnX = Math.min(mnX, p[0]); mnY = Math.min(mnY, p[1]);
                mxX = Math.max(mxX, p[0]); mxY = Math.max(mxY, p[1]);
            }
            this.minX = mnX; this.minY = mnY; this.maxX = mxX; this.maxY = mxY;
        }
    }

    /** SIG_CD → 폴리곤들 (MultiPolygon 은 여러 개) */
    private final Map<String, List<Poly>> bySig = new LinkedHashMap<>();

    @PostConstruct
    void load() {
        JsonNode root;
        try (InputStream in = new ClassPathResource("static/geo/sig.json").getInputStream()) {
            root = new ObjectMapper().readTree(in);
        } catch (Exception e) {
            log.error("[SigGeometry] sig.json 로드 실패: {}", e.getMessage());
            return;
        }

        JsonNode tr = root.get("transform");
        double sx = tr.get("scale").get(0).doubleValue();
        double sy = tr.get("scale").get(1).doubleValue();
        double tx = tr.get("translate").get(0).doubleValue();
        double ty = tr.get("translate").get(1).doubleValue();

        JsonNode arcsNode = root.get("arcs");
        double[][][] arcs = new double[arcsNode.size()][][];
        for (int a = 0; a < arcsNode.size(); a++) {
            JsonNode arc = arcsNode.get(a);
            double[][] pts = new double[arc.size()][2];
            long qx = 0, qy = 0;
            for (int i = 0; i < arc.size(); i++) {
                qx += arc.get(i).get(0).intValue();
                qy += arc.get(i).get(1).intValue();
                pts[i][0] = qx * sx + tx;
                pts[i][1] = qy * sy + ty;
            }
            arcs[a] = pts;
        }

        JsonNode objects = root.get("objects");
        JsonNode geometries = objects.get(objects.propertyNames().iterator().next()).get("geometries");
        for (int g = 0; g < geometries.size(); g++) {
            JsonNode geom = geometries.get(g);
            String sigCd = geom.get("properties").get("SIG_CD").asString();
            String type = geom.get("type").asString();
            JsonNode arcsField = geom.get("arcs");
            List<Poly> polys = new ArrayList<>();
            if ("Polygon".equals(type)) {
                polys.add(toPoly(arcsField, arcs));
            } else if ("MultiPolygon".equals(type)) {
                for (int p = 0; p < arcsField.size(); p++) {
                    polys.add(toPoly(arcsField.get(p), arcs));
                }
            }
            bySig.put(sigCd, polys);
        }
        log.info("[SigGeometry] 폴리곤 로드 완료: {}개 지역", bySig.size());
    }

    private Poly toPoly(JsonNode ringArcsList, double[][][] arcs) {
        double[][] outer = ring(ringArcsList.get(0), arcs);
        List<double[][]> holes = new ArrayList<>();
        for (int i = 1; i < ringArcsList.size(); i++) {
            holes.add(ring(ringArcsList.get(i), arcs));
        }
        return new Poly(outer, holes);
    }

    private double[][] ring(JsonNode ringArcs, double[][][] arcs) {
        List<double[]> coords = new ArrayList<>();
        for (int k = 0; k < ringArcs.size(); k++) {
            int idx = ringArcs.get(k).intValue();
            boolean reversed = idx < 0;
            double[][] arc = reversed ? arcs[~idx] : arcs[idx];
            int len = arc.length;
            for (int i = 0; i < len; i++) {
                if (i == 0 && !coords.isEmpty()) continue;
                coords.add(arc[reversed ? (len - 1 - i) : i]);
            }
        }
        return coords.toArray(new double[0][]);
    }

    /* ---------- 판정 API ---------- */

    /** 좌표가 특정 SIG_CD 폴리곤 안에 있는지 */
    public boolean isInSigCd(String sigCd, double lng, double lat) {
        List<Poly> polys = bySig.get(sigCd);
        if (polys == null) return false;
        for (Poly p : polys) {
            if (inPoly(p, lng, lat)) return true;
        }
        return false;
    }

    /** 좌표가 속한 SIG_CD 판정 (sidoPrefix 로 후보를 앞 2자리로 제한 가능, null 이면 전체) */
    public Optional<String> resolveSigCd(double lng, double lat, String sidoPrefix) {
        for (Map.Entry<String, List<Poly>> e : bySig.entrySet()) {
            if (sidoPrefix != null && !e.getKey().startsWith(sidoPrefix)) continue;
            for (Poly p : e.getValue()) {
                if (inPoly(p, lng, lat)) return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }

    public Optional<String> resolveSigCd(double lng, double lat) {
        return resolveSigCd(lng, lat, null);
    }

    private boolean inPoly(Poly p, double x, double y) {
        if (x < p.minX || x > p.maxX || y < p.minY || y > p.maxY) return false;
        if (!rayCast(p.outer, x, y)) return false;
        for (double[][] h : p.holes) {
            if (rayCast(h, x, y)) return false; // 구멍 안이면 제외
        }
        return true;
    }

    private boolean rayCast(double[][] ring, double x, double y) {
        boolean in = false;
        for (int i = 0, j = ring.length - 1; i < ring.length; j = i++) {
            double xi = ring[i][0], yi = ring[i][1];
            double xj = ring[j][0], yj = ring[j][1];
            boolean intersect = ((yi > y) != (yj > y))
                    && (x < (xj - xi) * (y - yi) / (yj - yi) + xi);
            if (intersect) in = !in;
        }
        return in;
    }
}
