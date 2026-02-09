package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class MyCongestionResponseDto {
    private int userIdx;
    private String restName;
    private String status;
    private LocalDateTime createdAt;
}
