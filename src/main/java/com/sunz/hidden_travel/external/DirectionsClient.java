package com.sunz.hidden_travel.external;

import com.sunz.hidden_travel.config.KakaoMobilityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 카카오모빌리티 다중 경유지 길찾기.
 *
 * 좌표 목록을 주면 실제 도로를 따라가는 경로와 총 거리·소요시간을 돌려준다.
 * 키가 없거나 한도를 넘으면 null 을 반환하고, 화면은 직선 동선을 그대로 쓴다.
 */
@Component
public class DirectionsClient {

    private static final Logger log = LoggerFactory.getLogger(DirectionsClient.class);

    /** 사용량 기록에 쓰는 서비스 이름 */
    private static final String SERVICE = "route";

    /** 출발·도착을 뺀 경유지 상한 (API 제한) */
    private static final int MAX_WAYPOINTS = 28;

    private final KakaoMobilityProperties props;
    private final DailyCallBudget budget;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient client;

    public DirectionsClient(KakaoMobilityProperties props, DailyCallBudget budget) {
        this.props = props;
        this.budget = budget;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    public int remainingCalls() {
        return budget.remaining(SERVICE, props.getDailyCallLimit());
    }

    /** 좌표 1쌍 (경도, 위도) */
    public record Point(double lng, double lat) {}

    /**
     * 경로 결과.
     *
     * @param distanceMeters  총 이동 거리(m)
     * @param durationSeconds 총 소요 시간(초)
     * @param path            실제 도로를 따라가는 좌표열
     */
    public record Route(int distanceMeters, int durationSeconds, List<Point> path) {}

    /**
     * 순서대로 이어지는 경로를 구한다. 2곳 미만이거나 실패하면 null.
     */
    public Route route(List<Point> stops) {
        if (stops == null || stops.size() < 2) {
            return null;
        }
        if (!props.isConfigured()) {
            log.warn("[Directions] REST API 키가 없습니다. config/application-secret.yaml 의 kakao.mobility.rest-api-key 를 확인하세요.");
            return null;
        }
        if (!budget.reserve(SERVICE, props.getDailyCallLimit())) {
            log.warn("[Directions] 1일 호출 한도({}) 소진", props.getDailyCallLimit());
            return null;
        }

        List<Point> points = stops.size() > MAX_WAYPOINTS + 2
                ? stops.subList(0, MAX_WAYPOINTS + 2)   // 상한을 넘으면 앞쪽만 계산한다
                : stops;

        try {
            String json = client.post()
                    .uri(props.getEndpoint())
                    .header("Authorization", "KakaoAK " + props.getRestApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(body(points)))
                    .retrieve()
                    .body(String.class);
            return parse(json);
        } catch (Exception e) {
            log.warn("[Directions] 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    private ObjectNode body(List<Point> points) {
        ObjectNode root = mapper.createObjectNode();
        root.set("origin", point(points.get(0)));
        root.set("destination", point(points.get(points.size() - 1)));

        if (points.size() > 2) {
            ArrayNode waypoints = root.putArray("waypoints");
            for (Point p : points.subList(1, points.size() - 1)) {
                waypoints.add(point(p));
            }
        }
        root.put("priority", "RECOMMEND");
        root.put("car_fuel", "GASOLINE");
        root.put("alternatives", false);
        root.put("road_details", false);
        return root;
    }

    private ObjectNode point(Point p) {
        ObjectNode n = mapper.createObjectNode();
        n.put("x", p.lng());   // 경도
        n.put("y", p.lat());   // 위도
        return n;
    }

    /** routes[0] 의 summary(거리·시간)와 sections[].roads[].vertexes(좌표열)를 읽는다 */
    private Route parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode route = mapper.readTree(json).path("routes").path(0);
            int resultCode = route.path("result_code").asInt(-1);
            if (resultCode != 0) {
                log.warn("[Directions] 경로를 찾지 못했습니다. result_code={} msg={}",
                        resultCode, route.path("result_msg").asString());
                return null;
            }

            JsonNode summary = route.path("summary");
            int distance = summary.path("distance").asInt(0);
            int duration = summary.path("duration").asInt(0);

            // vertexes 는 [x1, y1, x2, y2, ...] 로 평탄화되어 온다
            List<Point> path = new ArrayList<>();
            for (JsonNode section : route.path("sections")) {
                for (JsonNode road : section.path("roads")) {
                    JsonNode v = road.path("vertexes");
                    for (int i = 0; i + 1 < v.size(); i += 2) {
                        path.add(new Point(v.get(i).asDouble(), v.get(i + 1).asDouble()));
                    }
                }
            }
            if (path.isEmpty()) {
                log.warn("[Directions] 좌표열이 비어 있습니다.");
                return null;
            }
            return new Route(distance, duration, path);
        } catch (Exception e) {
            log.warn("[Directions] 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
