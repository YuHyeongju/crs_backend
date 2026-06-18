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
    private int userCouponIdx;
    private String title;
    private String restName;
    private int pointCost;
    private LocalDate validUntil;
    private boolean used;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
}
