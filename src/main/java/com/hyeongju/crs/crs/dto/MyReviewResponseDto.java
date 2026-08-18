package com.hyeongju.crs.crs.dto;

import com.hyeongju.crs.crs.domain.Review;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
public class MyReviewResponseDto {
    // 마이페이지 "내가 쓴 리뷰 목록" 응답 DTO (어느 가게인지 알 수 있도록 식당 정보도 포함)

    private int reviewIdx;
    private int restIdx;
    private String restName;
    private String kakaoId;              // 지도 이동 등에 사용
    private int rating;
    private String content;
    private LocalDateTime reviewAt;
    private LocalDateTime reviewUpdateAt;

    public MyReviewResponseDto(Review review) {
        // Review 엔티티 -> DTO 필드 매핑
        this.reviewIdx = review.getReviewIdx();
        this.restIdx = review.getRestaurant().getRestIdx();
        this.restName = review.getRestaurant().getRestName();
        this.kakaoId = review.getRestaurant().getKakaoId();
        this.rating = review.getRating();
        this.content = review.getContent();
        this.reviewAt = review.getReviewAt();
        this.reviewUpdateAt = review.getReviewUpdateAt();
    }
}
