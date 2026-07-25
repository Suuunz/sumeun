package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.CoursePoint;
import com.sunz.hidden_travel.controller.dto.GoodPriceShop;
import com.sunz.hidden_travel.controller.dto.RegionOption;
import com.sunz.hidden_travel.controller.dto.RegionSummary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 퍼블리싱/프로토타입 단계용 지역 더미 데이터 제공자.
 * (DB 없음 — 하드코딩 + sig.json 지역명 로드)
 *
 * - 안동시 등 일부 지역은 풍부한 더미 데이터로 채운다.
 * - 그 외 코드는 sig.json 에서 로드한 실제 지역명 + "데이터 준비 중" 기본 응답을 준다.
 */
@Component
public class DummyRegionData {

    /** 시도 코드(앞 2자리) → 시도명 */
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

    /** 풍부한 더미 데이터가 있는 지역 (SIG_CD → RegionSummary) */
    private final Map<String, RegionSummary> regions = new LinkedHashMap<>();
    /** sig.json 에서 로드한 전체 250개 SIG_CD → 지역명 */
    private final Map<String, String> nameByCode = new LinkedHashMap<>();

    public DummyRegionData() {
        loadNames();
        seed();
    }

    /**
     * sig.json(TopoJSON)에서 SIG_CD/SIG_KOR_NM 을 읽어 지역명 맵 구성.
     * Boot 4의 Jackson 3(tools.jackson.*) API 의존을 피하기 위해 정규식으로 파싱한다.
     * 각 피처가 SIG_CD·SIG_KOR_NM 을 정확히 하나씩 문서 순서대로 가지므로 i번째끼리 짝지으면 정확하다.
     */
    private void loadNames() {
        try (InputStream in = new ClassPathResource("static/geo/sig.json").getInputStream()) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<String> codes = findAll(content, "\"SIG_CD\"\\s*:\\s*\"(\\d+)\"");
            List<String> names = findAll(content, "\"SIG_KOR_NM\"\\s*:\\s*\"([^\"]+)\"");
            int n = Math.min(codes.size(), names.size());
            for (int i = 0; i < n; i++) {
                nameByCode.put(codes.get(i), names.get(i));
            }
        } catch (Exception e) {
            // 지역명 맵이 없어도 서비스는 기본 응답으로 동작
            System.err.println("[DummyRegionData] sig.json 지역명 로드 실패: " + e.getMessage());
        }
    }

    private List<String> findAll(String content, String regex) {
        List<String> out = new ArrayList<>();
        Matcher m = Pattern.compile(regex).matcher(content);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /** SIG_CD 로 지역 정보 조회 (없으면 실제 지역명 기반 "데이터 준비 중" 응답) */
    public RegionSummary get(String sigCd) {
        RegionSummary r = regions.get(sigCd);
        return r != null ? r : notReady(sigCd);
    }

    /** 자동완성/빠른선택 옵션 = 풍부한 더미가 있는 지역들 */
    public List<RegionOption> options() {
        return regions.entrySet().stream()
                .map(e -> new RegionOption(e.getKey(), e.getValue().name()))
                .toList();
    }

    private String province(String sigCd) {
        if (sigCd == null || sigCd.length() < 2) return "";
        return SIDO.getOrDefault(sigCd.substring(0, 2), "");
    }

    private RegionSummary notReady(String sigCd) {
        String name = nameByCode.getOrDefault(sigCd, "데이터 준비 중");
        return new RegionSummary(
                name,
                province(sigCd),
                "이 지역의 여행 정보는 곧 준비될 예정이에요. 조금만 기다려 주세요.",
                List.of(), List.of(), List.of()
        );
    }

    /* =========================================================
       풍부한 더미 데이터
       ========================================================= */

    private void seed() {
        regions.put("47170", andong());
        regions.put("42770", jeongseon());
        regions.put("47730", uiseong());
        regions.put("47760", yeongyang());
        regions.put("47920", bonghwa());
    }

    private RegionSummary andong() {
        return new RegionSummary("안동시", "경상북도",
                "낙동강이 휘감아 도는 하회마을과 유교 문화의 정수를 간직한 도시. "
                        + "붐비지 않는 골목마다 전통과 미식이 조용히 숨어 있습니다.",
                List.of("안동간고등어", "안동찜닭", "안동소주", "헛제사밥", "안동포"),
                List.of(
                        new GoodPriceShop("소문난 국밥집", "돼지국밥", "7,000원", "한식", "안동시 옥동 123-4"),
                        new GoodPriceShop("할매 떡볶이", "떡볶이 1인분", "3,000원", "분식", "안동시 중앙로 45"),
                        new GoodPriceShop("만리장성", "짜장면", "5,000원", "중식", "안동시 태화동 78"),
                        new GoodPriceShop("정가네 보리밥", "보리밥 정식", "8,000원", "한식", "안동시 평화동 22"),
                        new GoodPriceShop("다방 연", "아메리카노", "2,500원", "카페", "안동시 삼산동 15"),
                        new GoodPriceShop("시장 손칼국수", "손칼국수", "6,000원", "한식", "안동시 신시장 내")
                ),
                List.of(
                        new CoursePoint(1, "하회마을", "관광명소", "유네스코 세계문화유산, 낙동강이 감싸 안은 전통 마을 산책", "10:00"),
                        new CoursePoint(2, "안동구시장", "전통시장", "안동찜닭 골목에서 향토 음식 맛보기", "12:00"),
                        new CoursePoint(3, "월영교", "자연/경관", "국내 최장 목책 인도교에서 달빛 산책", "17:00")
                ));
    }

    private RegionSummary jeongseon() {
        return new RegionSummary("정선군", "강원특별자치도",
                "태백산맥의 웅장한 산세에 둘러싸인 은둔의 명소. 정선 5일장의 향토 음식과 동강의 절경이 느린 여행을 부릅니다.",
                List.of("곤드레나물", "수리취떡", "황기", "찰옥수수"),
                List.of(
                        new GoodPriceShop("동박골식당", "곤드레밥 정식", "8,000원", "한식", "정선군 정선읍"),
                        new GoodPriceShop("회동집", "콧등치기 국수", "6,000원", "분식", "정선군 정선읍"),
                        new GoodPriceShop("정선면옥", "메밀 막국수", "7,000원", "한식", "정선군 정선읍")
                ),
                List.of(
                        new CoursePoint(1, "아라리촌", "관광명소", "정선의 옛 주거 문화를 재현한 테마 산책", "10:00"),
                        new CoursePoint(2, "정선 5일장", "전통시장", "향토 음식 맛보기 (끝자리 2·7일 운영)", "12:00"),
                        new CoursePoint(3, "병방치 스카이워크", "자연/경관", "한반도 지형을 닮은 동강의 비경 감상", "15:00")
                ));
    }

    private RegionSummary uiseong() {
        return new RegionSummary("의성군", "경상북도",
                "마늘 향 가득한 들판과 천년 고찰이 있는 조용한 고장.",
                List.of("의성마늘", "흑마늘", "의성 자두"),
                List.of(
                        new GoodPriceShop("의성 시장국밥", "소고기국밥", "8,000원", "한식", "의성군 의성읍")
                ),
                List.of(
                        new CoursePoint(1, "고운사", "사찰", "천년의 숲길을 품은 고찰", "10:30"),
                        new CoursePoint(2, "조문국 사적지", "유적", "삼한시대 고분군 산책", "14:00")
                ));
    }

    private RegionSummary yeongyang() {
        return new RegionSummary("영양군", "경상북도",
                "국제밤하늘보호공원, 별빛이 흐르는 오지 여행지.",
                List.of("영양고추", "산나물"),
                List.of(
                        new GoodPriceShop("선바위식당", "산채 정식", "9,000원", "한식", "영양군 영양읍")
                ),
                List.of(
                        new CoursePoint(1, "영양 자연생태공원", "자연", "반딧불이가 사는 청정 습지", "11:00"),
                        new CoursePoint(2, "두들마을", "전통마을", "음식디미방과 전통 한옥 골목", "15:00")
                ));
    }

    private RegionSummary bonghwa() {
        return new RegionSummary("봉화군", "경상북도",
                "백두대간 청정 산골, 협곡열차와 분천 산타마을의 겨울 낭만.",
                List.of("봉화송이", "봉화 한약우"),
                List.of(
                        new GoodPriceShop("춘양 기와식당", "한약우 국밥", "9,000원", "한식", "봉화군 봉화읍")
                ),
                List.of(
                        new CoursePoint(1, "분천역 산타마을", "관광명소", "백두대간 협곡열차의 출발지", "10:00"),
                        new CoursePoint(2, "청량산", "자연", "기암절벽과 청량사 트레킹", "13:30")
                ));
    }
}
