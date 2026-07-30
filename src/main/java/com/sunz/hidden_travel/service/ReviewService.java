package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.MyCourseCard;
import com.sunz.hidden_travel.controller.dto.ReviewCard;
import com.sunz.hidden_travel.controller.dto.ReviewDetail;
import com.sunz.hidden_travel.domain.AppUser;
import com.sunz.hidden_travel.domain.Region;
import com.sunz.hidden_travel.domain.Review;
import com.sunz.hidden_travel.domain.SavedCourse;
import com.sunz.hidden_travel.domain.SavedCourseStop;
import com.sunz.hidden_travel.repository.AppUserRepository;
import com.sunz.hidden_travel.repository.RegionRepository;
import com.sunz.hidden_travel.repository.ReviewRepository;
import com.sunz.hidden_travel.repository.SavedCourseRepository;
import com.sunz.hidden_travel.storage.ImageStorage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 후기 작성/조회 및 내 코스 목록 조립.
 * 지역 이름과 작성자 닉네임처럼 여러 엔티티에 흩어진 값을 화면용 DTO 로 묶는다.
 */
@Service
public class ReviewService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** 피드 카드에 노출할 본문 길이 */
    private static final int EXCERPT_LEN = 120;

    private final ReviewRepository reviewRepository;
    private final SavedCourseRepository savedCourseRepository;
    private final RegionRepository regionRepository;
    private final AppUserRepository appUserRepository;
    private final ImageStorage imageStorage;

    public ReviewService(ReviewRepository reviewRepository,
                         SavedCourseRepository savedCourseRepository,
                         RegionRepository regionRepository,
                         AppUserRepository appUserRepository,
                         ImageStorage imageStorage) {
        this.reviewRepository = reviewRepository;
        this.savedCourseRepository = savedCourseRepository;
        this.regionRepository = regionRepository;
        this.appUserRepository = appUserRepository;
        this.imageStorage = imageStorage;
    }

    /* =========================================================
       내 코스 목록
       ========================================================= */

    /** 내가 저장한 코스 + 각 코스의 후기 작성 여부 */
    @Transactional(readOnly = true)
    public List<MyCourseCard> myCourseCards(Long userId) {
        List<MyCourseCard> cards = new ArrayList<>();
        for (SavedCourse c : savedCourseRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            Long reviewId = reviewRepository.findFirstBySavedCourseId(c.getId())
                    .map(Review::getId).orElse(null);
            cards.add(new MyCourseCard(
                    c.getId(), c.getTitle(), c.getSigCd(), regionLabel(c.getSigCd()),
                    c.stopCount(), c.getGoodPriceCount(),
                    c.getStops().stream().map(SavedCourseStop::getName).toList(),
                    format(c.getCreatedAt()), reviewId));
        }
        return cards;
    }

    /* =========================================================
       후기 작성
       ========================================================= */

    /**
     * 후기를 저장한다(코스당 1건 — 이미 있으면 내용을 갱신하고 사진은 덧붙인다).
     *
     * @return 저장된 후기, 코스가 없거나 본문이 비어 있으면 null
     */
    @Transactional
    public Review write(Long userId, Long courseId, String content, List<MultipartFile> photos, boolean shared) {
        SavedCourse course = savedCourseRepository.findById(courseId).orElse(null);
        if (course == null || content == null || content.isBlank()) {
            return null;
        }

        Review review = reviewRepository.findFirstBySavedCourseId(courseId).orElseGet(Review::new);
        review.setSavedCourseId(courseId);
        review.setUserId(userId);
        review.setSigCd(course.getSigCd());
        review.setContent(content.trim());
        review.setShared(shared);
        review.getPhotoPaths().addAll(imageStorage.saveAll(photos));
        return reviewRepository.save(review);
    }

    /* =========================================================
       후기 조회
       ========================================================= */

    /** 후기 피드 — 공개된 후기만 최신순 */
    @Transactional(readOnly = true)
    public List<ReviewCard> feed() {
        List<ReviewCard> cards = new ArrayList<>();
        for (Review r : reviewRepository.findBySharedTrueOrderByCreatedAtDesc()) {
            SavedCourse course = savedCourseRepository.findById(r.getSavedCourseId()).orElse(null);
            cards.add(new ReviewCard(
                    r.getId(), nickname(r.getUserId()), r.getSigCd(), regionLabel(r.getSigCd()),
                    course != null ? course.getTitle() : "삭제된 코스",
                    r.coverPhoto(), r.getPhotoPaths().size(),
                    excerpt(r.getContent()), format(r.getCreatedAt())));
        }
        return cards;
    }

    /** 후기 상세. 없으면 null */
    @Transactional(readOnly = true)
    public ReviewDetail detail(Long reviewId, Long viewerId) {
        Review r = reviewRepository.findById(reviewId).orElse(null);
        if (r == null) {
            return null;
        }
        SavedCourse course = savedCourseRepository.findById(r.getSavedCourseId()).orElse(null);
        return new ReviewDetail(
                r.getId(),
                r.getSavedCourseId(),
                nickname(r.getUserId()),
                r.getSigCd(),
                regionLabel(r.getSigCd()),
                course != null ? course.getTitle() : "삭제된 코스",
                course != null ? course.getStops().stream().map(SavedCourseStop::getName).toList() : List.of(),
                List.copyOf(r.getPhotoPaths()),
                r.getContent(),
                format(r.getCreatedAt()),
                r.isShared(),
                r.getUserId().equals(viewerId));
    }

    /** 후기 작성 화면용 — 대상 코스(내 것이 아니면 null) */
    @Transactional(readOnly = true)
    public SavedCourse courseForWriting(Long courseId, Long userId) {
        SavedCourse course = savedCourseRepository.findById(courseId).orElse(null);
        return (course != null && course.getUserId().equals(userId)) ? course : null;
    }

    /** 이미 쓴 후기(수정 진입용). 없으면 null */
    @Transactional(readOnly = true)
    public Review existingReview(Long courseId) {
        return reviewRepository.findFirstBySavedCourseId(courseId).orElse(null);
    }

    /* =========================================================
       내부 helper
       ========================================================= */

    public String regionLabel(String sigCd) {
        Region region = regionRepository.findById(sigCd).orElse(null);
        return region != null ? region.getProvince() + " " + region.getName() : "";
    }

    private String nickname(Long userId) {
        return appUserRepository.findById(userId).map(AppUser::getNickname).orElse("여행자");
    }

    private String format(LocalDateTime at) {
        return at != null ? at.format(DATE) : "";
    }

    private String excerpt(String content) {
        if (content == null) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= EXCERPT_LEN ? flat : flat.substring(0, EXCERPT_LEN) + "…";
    }
}
