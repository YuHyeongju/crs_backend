package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MenuResponseDto {
    // 식당 상세 조회 응답에 포함되는 메뉴 1건 DTO
    private int menuIdx;
    private String menuName;
    private Integer menuPrice;
    private String imageUrl;    // 서버가 접근 가능한 전체 URL로 변환되어 내려감
}
