package com.sunz.hidden_travel.mbti;

/**
 * 여행 MBTI 문항 1개.
 *
 * @param axis    어떤 축을 재는 문항인지 (EI / SN / TF / JP)
 * @param letter1 1번 답을 고르면 얻는 글자
 * @param letter2 2번 답을 고르면 얻는 글자
 */
public record TravelMbtiQuestion(
        int number,
        String question,
        String answer1,
        String answer2,
        String axis,
        char letter1,
        char letter2
) {}
