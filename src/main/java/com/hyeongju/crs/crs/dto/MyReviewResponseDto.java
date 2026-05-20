package com.hyeongju.crs.crs.dto;

import com.hyeongju.crs.crs.domain.Review;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
public class MyReviewResponseDto {

    private int reviewIdx;
    private int restIdx;
    private String restName;
    private String kakaoId;
    private int rating;
    private String content;
    private LocalDateTime reviewAt;
    private LocalDateTime reviewUpdateAt;

    public MyReviewResponseDto(Review review) {
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
