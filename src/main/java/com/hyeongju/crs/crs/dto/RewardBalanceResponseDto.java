package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RewardBalanceResponseDto {
    private int balance; // 보유 포인트 총합
}
