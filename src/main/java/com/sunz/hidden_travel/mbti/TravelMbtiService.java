package com.sunz.hidden_travel.mbti;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 여행 MBTI 문항과 채점.
 *
 * 12문항을 네 축에 나눠 담았다 — 축마다 문항 수가 다르다.
 *   J/P 4문항 (Q1~Q4)  · S/N 2문항 (Q5~Q6)
 *   T/F 3문항 (Q7~Q9)  · E/I 3문항 (Q10~Q12)
 *
 * S/N 은 문항이 2개라 1:1 동점이 날 수 있다. 이때는 N 으로 정한다 —
 * 덜 알려진 곳을 찾아 나서는 서비스 성격에 맞춘 기본값이다.
 * (나머지 축은 홀수이거나 4문항이라 2:2 동점 시 같은 규칙으로 뒤 글자를 택한다)
 */
@Service
public class TravelMbtiService {

    public static final int QUESTION_COUNT = 12;

    private static final List<TravelMbtiQuestion> QUESTIONS = List.of(
            new TravelMbtiQuestion(1, "여행을 떠날 때 계획은",
                    "내가 걷는 길이 곧 여행코스", "계획은 필수", "JP", 'P', 'J'),
            new TravelMbtiQuestion(2, "여행 경비는",
                    "당장 국제거지만 안되면 되지!", "걸어다니는 계산기로 변신", "JP", 'P', 'J'),
            new TravelMbtiQuestion(3, "여행을 다녀온 후",
                    "홈스윗홈.. 침대로 점프!", "캐리어를 열고 물건을 정리한다", "JP", 'P', 'J'),
            new TravelMbtiQuestion(4, "여행지에서 식사할 때",
                    "유~명한 맛집을 작정하고 노리는 헌터", "처음 본 순간 사랑에 빠진 길거리 가게", "JP", 'J', 'P'),

            new TravelMbtiQuestion(5, "여행지에서 길을 잃었을 때",
                    "왔던 길로 돌아가는 헨젤과 그레텔st.", "자꾸 걸어 나가면 길이 있겠지, 지구는 둥그니까", "SN", 'S', 'N'),
            new TravelMbtiQuestion(6, "화려한 건축물을 보며 드는 생각은",
                    "\"어떤 방법으로 지었을까?\" 고민한다", "\"와 멋있다...\" 감탄한다", "SN", 'S', 'N'),

            new TravelMbtiQuestion(7, "아침에 늦잠 잔 친구에게",
                    "\"여행이 역시 피곤하지.\"", "\"내일은 시간 지키자.\"", "TF", 'F', 'T'),
            new TravelMbtiQuestion(8, "친구에게 차 사고가 났다고 전화 왔을 때 나의 대답은",
                    "\"괜찮아? ㅠㅠ 다친 데는 없어?\"", "\"보험 들었어?\"", "TF", 'F', 'T'),
            new TravelMbtiQuestion(9, "친구가 쓸데없는 기념품을 살 때",
                    "\"그래 니가 행복하다면...\"", "\"그거 결국 쓰레기 된다\"", "TF", 'F', 'T'),

            new TravelMbtiQuestion(10, "나는 여행지를 선택할 때 주로",
                    "사람이 많은 도시로", "나무가 많은 자연으로", "EI", 'E', 'I'),
            new TravelMbtiQuestion(11, "숙소를 구할 때",
                    "저녁에 바비큐 파티를 여는 곳", "조용하고 아늑한 곳", "EI", 'E', 'I'),
            new TravelMbtiQuestion(12, "여행지에 대한 감상을",
                    "말로 내뱉어야 직성이 풀린다", "내 마음 속에 저_장, 마음에 담고 느낀다", "EI", 'E', 'I')
    );

    public List<TravelMbtiQuestion> questions() {
        return QUESTIONS;
    }

    /**
     * 답안으로 유형을 계산한다.
     *
     * @param answers 문항 순서대로 1 또는 2. 길이가 문항 수와 다르거나 값이 이상하면 null
     */
    public TravelMbtiType score(List<Integer> answers) {
        if (answers == null || answers.size() != QUESTIONS.size()) {
            return null;
        }

        int ei = 0, sn = 0, tf = 0, jp = 0;   // 양수면 앞 글자(E/S/T/J), 음수면 뒤 글자(I/N/F/P)
        for (int i = 0; i < QUESTIONS.size(); i++) {
            Integer pick = answers.get(i);
            if (pick == null || (pick != 1 && pick != 2)) {
                return null;
            }
            TravelMbtiQuestion q = QUESTIONS.get(i);
            char letter = pick == 1 ? q.letter1() : q.letter2();

            switch (q.axis()) {
                case "EI" -> ei += (letter == 'E') ? 1 : -1;
                case "SN" -> sn += (letter == 'S') ? 1 : -1;
                case "TF" -> tf += (letter == 'T') ? 1 : -1;
                case "JP" -> jp += (letter == 'J') ? 1 : -1;
                default -> { /* 정의된 축만 쓴다 */ }
            }
        }

        // 동점(0)이면 뒤 글자를 택한다 — 탐험 쪽으로 기우는 기본값
        String code = "" +
                (ei > 0 ? 'E' : 'I') +
                (sn > 0 ? 'S' : 'N') +
                (tf > 0 ? 'T' : 'F') +
                (jp > 0 ? 'J' : 'P');

        return TravelMbtiType.of(code);
    }
}
