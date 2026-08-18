package com.hyeongju.crs.crs.dto;

import com.hyeongju.crs.crs.domain.ReviewReport;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReportResponseDto {
    // 관리자 화면의 "리뷰 신고 목록" 응답 DTO. 신고 정보 + 신고자 정보 + 신고당한 리뷰 정보를 한 번에 담음
    private int reportIdx;
    private LocalDateTime reportAt;
    private String reason;
    private String status;                  // PENDING/APPROVED/REJECTED
    private int reporterUserIdx;
    private String reporterName;
    private int reportedReviewIdx;
    private String reportedReviewContent;
    private int reportedReviewRating;
    private String reportedReviewAuthorName;

    public ReviewReportResponseDto(ReviewReport report) {
        // ReviewReport 엔티티 -> DTO 필드 매핑 (신고자/신고당한 리뷰 정보까지 함께 펼침)
        this.reportIdx = report.getReportIdx();
        this.reportAt = report.getReportAt();
        this.reason = report.getReason();
        this.status = report.getStatus();
        this.reporterUserIdx = report.getReporter().getUserIdx();
        this.reporterName = report.getReporter().getName();
        this.reportedReviewIdx = report.getReportedReview().getReviewIdx();
        this.reportedReviewContent = report.getReportedReview().getContent();
        this.reportedReviewRating = report.getReportedReview().getRating();
        this.reportedReviewAuthorName = report.getReportedReview().getUser().getName();
    }
}
