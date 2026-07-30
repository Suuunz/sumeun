package com.sunz.hidden_travel.service;

import com.sunz.hidden_travel.controller.dto.AttractionDetail;
import com.sunz.hidden_travel.domain.Attraction;
import com.sunz.hidden_travel.repository.AttractionRepository;
import com.sunz.hidden_travel.tour.TourApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/**
 * 관광지 상세를 <b>필요할 때만</b> TourAPI 에서 가져와 DB 에 캐시한다.
 *
 * 상세(detailCommon2 + detailIntro2)는 콘텐츠 1건당 호출 2회가 든다.
 * 전국 관광지 6,768건을 전량 적재하면 13,000회가 넘어 1일 한도(1000회)로는
 * 2주가 걸린다. 그래서 사용자가 카드를 펼쳐 본 관광지만 채운다.
 * 한 번 가져오면 DB 에 남으므로 두 번째부터는 호출이 들지 않는다.
 */
@Service
public class AttractionDetailService {

    private static final Logger log = LoggerFactory.getLogger(AttractionDetailService.class);

    private static final int CT_ATTRACTION = 12;

    /** 상세 조회에 필요한 최소 잔여 예산 (2회 + 여유) */
    private static final int MIN_REMAINING = 5;

    private final AttractionRepository attractionRepository;
    private final TourApiClient client;

    public AttractionDetailService(AttractionRepository attractionRepository, TourApiClient client) {
        this.attractionRepository = attractionRepository;
        this.client = client;
    }

    /** 상세 조회. 없는 관광지면 null */
    @Transactional
    public AttractionDetail detail(Long attractionId) {
        Attraction a = attractionRepository.findById(attractionId).orElse(null);
        if (a == null) {
            return null;
        }

        boolean pending = false;
        if (!a.isDetailFetched()) {
            if (a.getSourceContentId() == null || a.getSourceContentId().isBlank()) {
                // 외부 콘텐츠가 아니면 더 가져올 게 없다
                a.setDetailFetched(true);
            } else if (client.remainingCalls() < MIN_REMAINING) {
                // 한도가 없으면 이번엔 이름·주소·이미지만 돌려주고, 다음 기회에 다시 시도한다
                log.warn("[AttractionDetail] 호출 한도 부족 — 상세 조회를 건너뜁니다. id={}", attractionId);
                pending = true;
            } else {
                fetchInto(a);
            }
        }

        return new AttractionDetail(
                a.getId(), a.getName(), a.getAddr(), a.getImage(),
                a.getDescription(), a.getHomepage(), a.getUsetime(), a.getRestdate(),
                a.getParking(), a.getInfocenter(), a.getTel(), pending);
    }

    /** detailCommon2 + detailIntro2 를 읽어 엔티티에 채운다(최대 2회 호출) */
    private void fetchInto(Attraction a) {
        String cid = a.getSourceContentId();
        try {
            JsonNode common = client.detailCommon(cid);
            if (common != null) {
                a.setDescription(clean(text(common, "overview")));
                a.setHomepage(firstUrl(text(common, "homepage")));
                if (isBlank(a.getImage())) {
                    a.setImage(firstNonBlank(text(common, "firstimage"), text(common, "firstimage2")));
                }
            }

            JsonNode intro = client.detailIntro(cid, CT_ATTRACTION);
            if (intro != null) {
                a.setUsetime(clean(text(intro, "usetime")));
                a.setRestdate(clean(text(intro, "restdate")));
                a.setParking(clean(text(intro, "parking")));
                a.setInfocenter(clean(text(intro, "infocenter")));
            }
        } catch (Exception e) {
            log.warn("[AttractionDetail] 상세 조회 실패 contentId={}: {}", cid, e.getMessage());
        }
        // 결과가 비어 있어도 true — 같은 콘텐츠를 반복 호출하지 않는다
        a.setDetailFetched(true);
    }

    /* ---------- 유틸 ---------- */

    private String text(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? null : n.asString();
    }

    /** TourAPI 본문에는 &lt;br&gt; 등 태그가 섞여 온다 — 화면에서 그대로 보여줄 수 있게 정리 */
    private String clean(String s) {
        if (s == null) {
            return null;
        }
        String out = s.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim();
        return out.isBlank() ? null : out;
    }

    /** homepage 필드는 &lt;a href="..."&gt; 형태로 오는 경우가 많다 */
    private String firstUrl(String raw) {
        if (raw == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("https?://[^\"'\\s<>]+").matcher(raw);
        return m.find() ? m.group() : clean(raw);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String firstNonBlank(String a, String b) {
        return !isBlank(a) ? a : (!isBlank(b) ? b : null);
    }
}
