package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewReportRequestDto {
    // 리뷰 신고 요청 바디 DTO
    private int reviewIdx;
    private int reporterUserIdx;
    private String reason;
}
