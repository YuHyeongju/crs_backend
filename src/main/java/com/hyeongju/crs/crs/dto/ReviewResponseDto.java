package com.hyeongju.crs.crs.dto;

import com.hyeongju.crs.crs.domain.Review;
import com.hyeongju.crs.crs.domain.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter@Setter
@NoArgsConstructor
public class ReviewResponseDto {

    private int reviewIdx;
    private String content;
    private int rating;
    private LocalDateTime reviewAt;
    private int userIdx;

    public ReviewResponseDto(Review review){
        this.reviewIdx = review.getReviewIdx();
        this.content = review.getContent();
        this.rating = review.getRating();
        this.reviewAt = review.getReviewAt();
        this.userIdx = review.getUser().getUserIdx();
    }
}
