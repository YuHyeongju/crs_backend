package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequestDto {
    private int merchantUserIdx; // 등록 요청 상인 (가게 소유 검증용)
    private int restIdx;         // 쿠폰을 등록할 가게

    @NotBlank(message = "쿠폰 제목은 필수 입력 값입니다.")
    private String title;
    private String description;
    private int pointCost;
    private LocalDate validUntil; // null 가능 (무기한)
}
