package com.sunz.hidden_travel.controller.dto;

/**
 * 지도 화면 왼쪽의 '오늘의 숨은 여행지' 카드.
 *
 * 지도는 랭킹이 없어 모든 지역이 동등한 진입점이지만, 그래서 처음 온 사용자에겐
 * 시작점도 없다. 이 카드가 첫 삽을 떠주되 무작위로 순환해 유명지 쏠림은 만들지 않는다.
 *
 * @param heroName    사진 속 관광지 이름
 * @param description 지역 소개 — 적재된 관광지·특산물로 서버가 구성한다
 *                    (TourAPI 상세는 콘텐츠당 호출이 들어 카드에 쓰지 않는다)
 */
public record SpotlightCard(
        String sigCd,
        String name,
        String province,
        String image,
        String heroName,
        String description,
        int attractionCount,
        int shopCount
) {}
