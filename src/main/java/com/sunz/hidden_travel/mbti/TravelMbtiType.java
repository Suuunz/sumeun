package com.sunz.hidden_travel.mbti;

import java.util.Arrays;

/**
 * 여행 MBTI 16유형. 화면(프로필·후기)에 뱃지로 붙고, AI 상담의 입력으로도 쓰인다.
 *
 * label 은 유형을 한 줄로 보여주는 별명, tagline 은 뱃지 옆 짧은 설명,
 * style 은 AI 상담이 추천에 참고하는 여행 성향 키워드.
 */
public enum TravelMbtiType {

    ISTJ("ISTJ", "계획표의 수호자", "🗂️", "정해둔 일정을 끝까지 지켜내는 타입",
            "검증된 명소, 예측 가능한 일정, 정보가 정확한 곳"),
    ISFJ("ISFJ", "조용한 배려 여행자", "🧸", "함께 온 사람을 먼저 챙기는 타입",
            "한적하고 편안한 곳, 무리 없는 동선, 아늑한 숙소"),
    INFJ("INFJ", "의미를 찾는 순례자", "🕯️", "그곳의 이야기를 알고 싶어 하는 타입",
            "역사·문화가 깊은 곳, 사연이 있는 장소, 조용한 사색"),
    INTJ("INTJ", "완벽한 동선 설계자", "🗺️", "지도를 펼쳐 최적 경로를 짜는 타입",
            "효율적인 동선, 밀도 높은 일정, 붐비지 않는 시간대"),

    ISTP("ISTP", "말없이 떠나는 탐험가", "🧭", "발길 닿는 대로 움직이는 타입",
            "즉흥적인 이동, 자연 속 활동, 사람 적은 곳"),
    ISFP("ISFP", "감성 산책자", "🍃", "예쁜 순간을 조용히 담는 타입",
            "풍경이 좋은 산책길, 감성적인 카페, 사진 찍기 좋은 곳"),
    INFP("INFP", "마음에 담는 몽상가", "🌙", "여운을 오래 간직하는 타입",
            "고요한 자연, 오래된 골목, 혼자 걷기 좋은 길"),
    INTP("INTP", "호기심 많은 관찰자", "🔍", "왜 그런지 파고드는 타입",
            "박물관·유적, 독특한 구조물, 설명이 있는 장소"),

    ESTP("ESTP", "부딪히며 배우는 모험가", "🔥", "일단 가보고 판단하는 타입",
            "체험 활동, 활기찬 시장, 즉흥적인 코스"),
    ESFP("ESFP", "순간을 즐기는 흥부자", "🎉", "지금 이 순간이 제일 중요한 타입",
            "축제와 야시장, 사람 많은 거리, 먹거리 골목"),
    ENFP("ENFP", "우연을 사랑하는 방랑자", "🎈", "계획에 없던 발견을 반기는 타입",
            "골목 탐방, 우연히 만난 가게, 다양한 경험"),
    ENTP("ENTP", "새로움을 찾는 탐구가", "💡", "남들 안 가본 곳에 끌리는 타입",
            "덜 알려진 지역, 독특한 테마, 새로운 시도"),

    ESTJ("ESTJ", "완주하는 여행 리더", "🚩", "계획한 건 다 해내는 타입",
            "알찬 일정, 대표 명소 위주, 시간 효율"),
    ESFJ("ESFJ", "함께라서 즐거운 동행자", "🤝", "같이 웃는 게 좋은 타입",
            "함께 즐기는 활동, 맛집 탐방, 인기 있는 코스"),
    ENFJ("ENFJ", "모두를 챙기는 인솔자", "☀️", "일행 모두가 만족하길 바라는 타입",
            "누구나 좋아할 만한 곳, 이야깃거리 있는 장소, 무난한 난이도"),
    ENTJ("ENTJ", "목표를 정복하는 전략가", "⚡", "가기로 한 곳은 반드시 가는 타입",
            "핵심 명소 집중, 촘촘한 계획, 이동 효율");

    private final String code;
    private final String label;
    private final String emoji;
    private final String tagline;
    private final String style;

    TravelMbtiType(String code, String label, String emoji, String tagline, String style) {
        this.code = code;
        this.label = label;
        this.emoji = emoji;
        this.tagline = tagline;
        this.style = style;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getTagline() {
        return tagline;
    }

    /** AI 상담이 추천에 참고하는 성향 키워드 */
    public String getStyle() {
        return style;
    }

    /** 코드로 찾기. 없거나 형식이 다르면 null */
    public static TravelMbtiType of(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String c = code.trim().toUpperCase();
        return Arrays.stream(values()).filter(t -> t.code.equals(c)).findFirst().orElse(null);
    }
}
