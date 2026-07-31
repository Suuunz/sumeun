package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.CourseRoute;
import com.sunz.hidden_travel.domain.SavedCourse;
import com.sunz.hidden_travel.domain.SavedCourseStop;
import com.sunz.hidden_travel.external.DirectionsClient;
import com.sunz.hidden_travel.repository.SavedCourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 저장된 코스의 실제 도로 경로를 구해 캐시한다.
 *
 * 경로는 코스당 한 번만 계산한다. 경유지가 바뀌지 않는 한 결과가 같고,
 * 길찾기 호출은 한도가 있어 페이지를 열 때마다 부를 이유가 없다.
 */
@Service
public class CourseRouteService {

    private static final Logger log = LoggerFactory.getLogger(CourseRouteService.class);

    private final SavedCourseRepository savedCourseRepository;
    private final DirectionsClient directions;

    public CourseRouteService(SavedCourseRepository savedCourseRepository, DirectionsClient directions) {
        this.savedCourseRepository = savedCourseRepository;
        this.directions = directions;
    }

    /**
     * 코스의 도로 경로. 없으면 계산해서 저장한 뒤 반환한다.
     *
     * @return 코스를 못 찾으면 null. 좌표가 부족하거나 키가 없으면 unavailable 상태로 반환.
     */
    @Transactional
    public CourseRoute route(Long courseId) {
        SavedCourse course = savedCourseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return null;
        }

        // 이미 계산해둔 경로가 있으면 그대로
        if (course.hasRoute()) {
            return new CourseRoute(course.distanceText(), course.durationText(),
                    decode(course.getRoutePath()), true, null);
        }

        List<DirectionsClient.Point> stops = course.getStops().stream()
                .filter(SavedCourseStop::hasCoord)
                .map(s -> new DirectionsClient.Point(s.getLng(), s.getLat()))
                .toList();

        if (stops.size() < 2) {
            return new CourseRoute(null, null, List.of(), false,
                    "좌표가 있는 장소가 두 곳 이상이어야 경로를 계산할 수 있어요.");
        }
        if (!directions.isConfigured()) {
            return new CourseRoute(null, null, List.of(), false,
                    "길찾기 키가 설정되지 않았어요. config/application-secret.yaml 의 kakao.mobility.rest-api-key 를 확인해 주세요.");
        }

        DirectionsClient.Route r = directions.route(stops);
        if (r == null) {
            return new CourseRoute(null, null, List.of(), false,
                    "지금은 경로를 계산하지 못했어요. 잠시 후 다시 시도해 주세요.");
        }

        course.setRouteDistanceMeters(r.distanceMeters());
        course.setRouteDurationSeconds(r.durationSeconds());
        course.setRoutePath(encode(r.path()));
        log.info("[CourseRoute] courseId={} 경로 계산 — {}m / {}초, 좌표 {}개",
                courseId, r.distanceMeters(), r.durationSeconds(), r.path().size());

        return new CourseRoute(course.distanceText(), course.durationText(),
                toPairs(r.path()), true, null);
    }

    /* ---------- 좌표열 직렬화 ---------- */

    /** "lng,lat lng,lat ..." — JSON 배열보다 짧아 TEXT 한 칸에 담기 좋다 */
    private String encode(List<DirectionsClient.Point> path) {
        StringBuilder sb = new StringBuilder();
        for (DirectionsClient.Point p : path) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p.lng()).append(',').append(p.lat());
        }
        return sb.toString();
    }

    private List<double[]> decode(String encoded) {
        List<double[]> out = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return out;
        }
        for (String pair : encoded.split(" ")) {
            String[] xy = pair.split(",");
            if (xy.length != 2) {
                continue;
            }
            try {
                out.add(new double[]{Double.parseDouble(xy[0]), Double.parseDouble(xy[1])});
            } catch (NumberFormatException e) {
                // 손상된 값은 건너뛴다
            }
        }
        return out;
    }

    private List<double[]> toPairs(List<DirectionsClient.Point> path) {
        List<double[]> out = new ArrayList<>(path.size());
        for (DirectionsClient.Point p : path) {
            out.add(new double[]{p.lng(), p.lat()});
        }
        return out;
    }
}
