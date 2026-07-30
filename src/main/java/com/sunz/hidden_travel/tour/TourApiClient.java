package com.sunz.hidden_travel.tour;

import com.sunz.hidden_travel.config.TourApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 한국관광공사 TourAPI(KorService2) 호출 클라이언트.
 * 국문 관광정보: areaBasedList2 / locationBasedList2 / detailInfo2
 */
@Component
public class TourApiClient {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient client;
    private final String serviceKey;
    private final int dailyLimit;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 오늘 사용한 호출 수 (날짜가 바뀌면 리셋) */
    private final AtomicInteger usedToday = new AtomicInteger();
    private volatile LocalDate usageDate = LocalDate.now(KST);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public TourApiClient(TourApiProperties props) {
        this.serviceKey = props.getKey();
        this.dailyLimit = props.getDailyCallLimit();
        this.client = RestClient.builder().baseUrl(props.getEndpoint()).build();
    }

    /* =========================================================
       호출 예산
       ========================================================= */

    /** 오늘 남은 호출 가능 횟수 */
    public synchronized int remainingCalls() {
        rollOverIfNewDay();
        return Math.max(0, dailyLimit - usedToday.get());
    }

    public int usedCalls() {
        rollOverIfNewDay();
        return usedToday.get();
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    /** 호출 1회를 예약한다. 한도를 넘으면 false — 호출하지 않는다. */
    private synchronized boolean reserveCall() {
        rollOverIfNewDay();
        if (usedToday.get() >= dailyLimit) {
            return false;
        }
        usedToday.incrementAndGet();
        return true;
    }

    private synchronized void rollOverIfNewDay() {
        LocalDate today = LocalDate.now(KST);
        if (!today.equals(usageDate)) {
            usageDate = today;
            usedToday.set(0);
            log.info("[TourAPI] 날짜 변경 — 호출 카운터를 리셋합니다({}).", today);
        }
    }

    /** 한 페이지 결과 */
    public record TourPage(List<JsonNode> items, int totalCount) {
        public static TourPage empty() {
            return new TourPage(List.of(), 0);
        }
    }

    /** 지역(시도) 기반 목록 */
    public TourPage areaBasedList(int areaCode, int contentTypeId, int pageNo, int numOfRows) {
        return call(b -> b.path("/areaBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "Sumeun")
                .queryParam("_type", "json")
                .queryParam("arrange", "O")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("areaCode", areaCode)
                .build());
    }

    /** 좌표(반경) 기반 목록 */
    public TourPage locationBasedList(double lng, double lat, int radiusMeters,
                                      int contentTypeId, int pageNo, int numOfRows) {
        return call(b -> b.path("/locationBasedList2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "Sumeun")
                .queryParam("_type", "json")
                .queryParam("arrange", "E")
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("contentTypeId", contentTypeId)
                .queryParam("mapX", lng)
                .queryParam("mapY", lat)
                .queryParam("radius", radiusMeters)
                .build());
    }

    /** 여행코스 세부 경유지 (contentTypeId=25) */
    public List<JsonNode> detailInfo(String contentId, int contentTypeId) {
        return call(b -> b.path("/detailInfo2")
                .queryParam("serviceKey", serviceKey)
                .queryParam("MobileOS", "ETC")
                .queryParam("MobileApp", "Sumeun")
                .queryParam("_type", "json")
                .queryParam("numOfRows", 50)
                .queryParam("pageNo", 1)
                .queryParam("contentId", contentId)
                .queryParam("contentTypeId", contentTypeId)
                .build()).items();
    }

    /* ---------- 내부: 호출 + 파싱 ---------- */
    private TourPage call(java.util.function.Function<org.springframework.web.util.UriBuilder, java.net.URI> uriFn) {
        // 한도를 넘으면 호출하지 않는다. 빈 페이지를 돌려주면 배치 루프가 스스로 멈춘다.
        if (!reserveCall()) {
            log.warn("[TourAPI] 1일 호출 한도({}) 소진 — 호출을 중단합니다. 내일 이어서 실행하세요.", dailyLimit);
            return TourPage.empty();
        }
        String json;
        try {
            json = client.get().uri(uriFn).retrieve().body(String.class);
        } catch (Exception e) {
            log.warn("[TourAPI] 호출 실패: {}", e.getMessage());
            return TourPage.empty();
        }
        if (json == null || json.isBlank()) return TourPage.empty();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode body = root.path("response").path("body");
            if (body.isMissingNode()) {
                log.warn("[TourAPI] 예상치 못한 응답(앞부분): {}", json.substring(0, Math.min(180, json.length())));
                return TourPage.empty();
            }
            int totalCount = body.path("totalCount").asInt(0);
            JsonNode items = body.path("items");
            List<JsonNode> list = new ArrayList<>();
            if (items.isObject()) {
                JsonNode item = items.path("item");
                if (item.isArray()) {
                    for (int i = 0; i < item.size(); i++) list.add(item.get(i));
                } else if (item.isObject()) {
                    list.add(item);
                }
            }
            return new TourPage(list, totalCount);
        } catch (Exception e) {
            log.warn("[TourAPI] 응답 파싱 실패: {} (앞부분: {})", e.getMessage(),
                    json.substring(0, Math.min(180, json.length())));
            return TourPage.empty();
        }
    }
}
