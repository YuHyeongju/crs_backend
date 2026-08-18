package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyCouponResponseDto {
    // 마이페이지 "내가 보유한 쿠폰 목록" 응답 DTO
    private int userCouponIdx;
    private String title;
    private String restName;
    private int pointCost;
    private LocalDate validUntil;
    private boolean used;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;   // 미사용이면 null
}
