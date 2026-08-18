package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponseDto {
    // 쿠폰 목록 조회 응답 DTO
    private int couponIdx;
    private int restIdx;
    private String restName;
    private String title;
    private String description;
    private int pointCost;
    private LocalDate validUntil; // null이면 무기한
    private boolean active;
}
