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
    private int reportIdx;
    private LocalDateTime reportAt;
    private String reason;
    private String status;
    private int reporterUserIdx;
    private String reporterName;
    private int reportedReviewIdx;
    private String reportedReviewContent;
    private int reportedReviewRating;
    private String reportedReviewAuthorName;

    public ReviewReportResponseDto(ReviewReport report) {
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
