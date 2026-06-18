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
    private int couponIdx;
    private int restIdx;
    private String restName;
    private String title;
    private String description;
    private int pointCost;
    private LocalDate validUntil;
    private boolean active;
}
