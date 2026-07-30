package com.sunz.hidden_travel.ai;

import com.sunz.hidden_travel.config.GeminiProperties;
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
import java.util.List;

/**
 * Gemini generateContent 호출 클라이언트.
 *
 * 키가 없으면 호출하지 않고 null 을 돌려준다 — 화면이 "키를 설정해 주세요"로 안내한다.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final GeminiProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient client;

    public GeminiClient(GeminiProperties props) {
        this.props = props;
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(props.getTimeoutSeconds()));
        this.client = RestClient.builder()
                .baseUrl(props.getEndpoint())
                .requestFactory(factory)
                .build();
    }

    public boolean isConfigured() {
        return props.isConfigured();
    }

    /** 대화 1턴 */
    public record Turn(String role, String text) {
        public static Turn user(String text) {
            return new Turn("user", text);
        }
        public static Turn model(String text) {
            return new Turn("model", text);
        }
    }

    /**
     * 대화를 보내고 모델의 응답 텍스트를 받는다.
     *
     * @param systemInstruction 역할·규칙·데이터 카탈로그
     * @param history           이전 대화 + 이번 사용자 발화
     * @return 응답 텍스트, 실패하면 null
     */
    public String generate(String systemInstruction, List<Turn> history) {
        if (!props.isConfigured()) {
            log.warn("[Gemini] API 키가 설정되지 않았습니다. config/application-secret.yaml 의 gemini.api.key 를 확인하세요.");
            return null;
        }

        ObjectNode body = mapper.createObjectNode();

        // 시스템 지시
        ObjectNode sys = body.putObject("systemInstruction");
        sys.putArray("parts").addObject().put("text", systemInstruction);

        // 대화 내역
        ArrayNode contents = body.putArray("contents");
        for (Turn t : history) {
            ObjectNode c = contents.addObject();
            c.put("role", t.role());
            c.putArray("parts").addObject().put("text", t.text());
        }

        // JSON 으로만 답하도록 강제 — 파싱 실패를 줄인다
        ObjectNode cfg = body.putObject("generationConfig");
        cfg.put("temperature", 0.7);
        cfg.put("maxOutputTokens", 1024);
        cfg.put("responseMimeType", "application/json");

        try {
            String json = client.post()
                    .uri("/models/{model}:generateContent?key={key}", props.getModel(), props.getKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

            return extractText(json);
        } catch (Exception e) {
            log.warn("[Gemini] 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /** candidates[0].content.parts[*].text 를 이어붙인다 */
    private String extractText(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                log.warn("[Gemini] 예상치 못한 응답: {}", json.substring(0, Math.min(300, json.length())));
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                String t = p.path("text").asString();
                if (t != null) {
                    sb.append(t);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("[Gemini] 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}
