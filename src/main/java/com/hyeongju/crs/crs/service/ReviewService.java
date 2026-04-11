package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.Review;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.ReviewRequestDto;
import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Getter@Setter
@RequiredArgsConstructor
public class ReviewService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public List<ReviewResponseDto> getReviewsByRestaurant(int restIdx){
        List<Review> reviews = reviewRepository.findByRestaurant_RestIdxOrderByReviewAtDesc(restIdx);
        System.out.println("리뷰 가져오기 성공");
        System.out.println("가져온 리뷰 개수: " +  reviews.size() + "개");

        return reviews.stream().map(ReviewResponseDto :: new).collect(Collectors.toList());
    }
    @Transactional
    public void saveReview(ReviewRequestDto reviewRequestDto){

        Restaurant restaurant = restaurantRepository.findByRestIdx(reviewRequestDto.getRestIdx())
                .orElseThrow(()-> new IllegalStateException("가게를 찾을 수 없습니다"));

        User user = userRepository.findByUserIdx(reviewRequestDto.getUserIdx())
                .orElseThrow(()-> new IllegalStateException("유저를 찾을 수 없습니다."));

        Review review = new Review();
        review.setRestaurant(restaurant);
        review.setUser(user);
        review.setContent(reviewRequestDto.getContent());
        review.setRating(reviewRequestDto.getRating());
        review.setReviewAt(LocalDateTime.now());

        reviewRepository.save(review);
    }

}
