package com.hyeongju.crs.crs.dto;

import com.hyeongju.crs.crs.domain.Review;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter@Setter
@NoArgsConstructor
public class ReviewResponseDto {
    // 리뷰 목록/상세 조회 응답 DTO. Review 엔티티를 그대로 반환하지 않고 필요한 필드만 추려서 내려줌

    private int reviewIdx;
    private String content;
    private int rating;
    private LocalDateTime reviewAt;
    private int userIdx;
    private String userName;

    public ReviewResponseDto(Review review){
        // Review 엔티티 -> DTO 필드 매핑
        this.reviewIdx = review.getReviewIdx();
        this.content = review.getContent();
        this.rating = review.getRating();
        this.reviewAt = review.getReviewAt();
        this.userIdx = review.getUser().getUserIdx();
        this.userName = review.getUser().getName();

    }
}
