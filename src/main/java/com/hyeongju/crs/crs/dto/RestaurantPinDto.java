package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantPinDto {
    // 지도 위에 식당 핀을 찍기 위한 응답 DTO (위경도 + 요약 정보만, 메뉴/편의시설 등은 제외)
    private int restIdx;
    private String restName;
    private String restAddress;
    private String restTel;
    private Double latitude;
    private Double longitude;
    private String kakaoId;
    private Double averageRating;
    private Integer reviewCount;
    private Integer ownerUserIdx;
}
