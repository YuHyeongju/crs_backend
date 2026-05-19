package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewReportRequestDto {
    private int reviewIdx;
    private int reporterUserIdx;
    private String reason;
}
