package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MenuResponseDto {
    private int menuIdx;
    private String menuName;
    private Integer menuPrice;
    private String imageUrl;
}
