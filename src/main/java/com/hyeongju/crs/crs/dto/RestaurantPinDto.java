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
