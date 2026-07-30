package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.CourseStopPayload;
import com.sunz.hidden_travel.domain.SavedCourse;
import com.sunz.hidden_travel.domain.SavedCourseStop;
import com.sunz.hidden_travel.repository.SavedCourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// Spring Boot 4 는 Jackson 3(tools.jackson) 을 쓴다 — com.fasterxml 패키지가 아니다.
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 내 코스 저장/조회. 코스 저장 완료 화면과 내 코스 목록·후기 기능의 데이터 원천.
 */
@Service
public class SavedCourseService {

    private static final Logger log = LoggerFactory.getLogger(SavedCourseService.class);
    private static final String GOOD_PRICE_TYPE = "goodprice";

    private final SavedCourseRepository savedCourseRepository;
    private final ObjectMapper objectMapper;

    public SavedCourseService(SavedCourseRepository savedCourseRepository, ObjectMapper objectMapper) {
        this.savedCourseRepository = savedCourseRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 코스를 저장한다. 경유지가 없으면 저장하지 않고 null 을 반환한다.
     *
     * @param itemsJson course.js 가 만든 JSON 배열 문자열
     */
    @Transactional
    public SavedCourse save(Long userId, String sigCd, String title, String itemsJson) {
        List<CourseStopPayload> payload = parse(itemsJson);
        if (payload.isEmpty()) {
            log.warn("[SavedCourse] 경유지가 없어 저장하지 않습니다. userId={}, sigCd={}", userId, sigCd);
            return null;
        }

        SavedCourse course = new SavedCourse();
        course.setUserId(userId);
        course.setSigCd(sigCd);
        course.setTitle(title != null && !title.isBlank() ? title.trim() : "나의 코스");

        int order = 1;
        int goodPrice = 0;
        for (CourseStopPayload p : payload) {
            if (p.name() == null || p.name().isBlank()) {
                continue;
            }
            course.getStops().add(new SavedCourseStop(order++, p.name(), p.type(), p.sage(), p.lat(), p.lng()));
            if (p.sage() || GOOD_PRICE_TYPE.equals(p.type())) {
                goodPrice++;
            }
        }
        if (course.getStops().isEmpty()) {
            return null;
        }
        course.setGoodPriceCount(goodPrice);
        return savedCourseRepository.save(course);
    }

    /** 내 코스 목록 (최근 저장 순) */
    @Transactional(readOnly = true)
    public List<SavedCourse> myCourses(Long userId) {
        return savedCourseRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public SavedCourse find(Long courseId) {
        return courseId == null ? null : savedCourseRepository.findById(courseId).orElse(null);
    }

    /** 잘못된 JSON 은 예외로 터뜨리지 않고 빈 목록으로 처리(저장만 실패) */
    private List<CourseStopPayload> parse(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<CourseStopPayload>>() {});
        } catch (Exception e) {
            log.warn("[SavedCourse] itemsJson 파싱 실패: {}", itemsJson, e);
            return List.of();
        }
    }
}
