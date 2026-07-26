package com.sunz.hidden_travel.tour;

import com.sunz.hidden_travel.config.TourApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 한국관광공사 TourAPI(KorService2) 호출 클라이언트.
 * 국문 관광정보: areaBasedList2 / locationBasedList2 / detailInfo2
 */
@Component
public class TourApiClient {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient client;
    private final String serviceKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public TourApiClient(TourApiProperties props) {
        this.serviceKey = props.getKey();
        this.client = RestClient.builder().baseUrl(props.getEndpoint()).build();
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
