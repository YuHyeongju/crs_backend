package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MyCongestionResponseDto {
    // 마이페이지 "내가 제보한 혼잡도 목록" 응답 DTO
    private int userIdx;
    private String restName;
    private String status;
    private LocalDateTime createdAt;
}
