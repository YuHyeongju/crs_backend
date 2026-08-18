package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponseDto {
    // 식당 목록/검색 결과 응답 DTO (평점 통계 포함)
    private Integer restIdx;
    private String restName;
    private String restAddress;
    private Double averageRating;  // 리뷰가 없으면 null일 수 있음
    private Integer reviewCount;
    private Integer ownerUserIdx;  // 카카오 자동생성 식당은 null
}
