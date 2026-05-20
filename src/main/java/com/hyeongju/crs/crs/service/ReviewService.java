package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.Review;
import com.hyeongju.crs.crs.domain.ReviewReport;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MyReviewResponseDto;
import com.hyeongju.crs.crs.dto.ReviewReportRequestDto;
import com.hyeongju.crs.crs.dto.ReviewRequestDto;
import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewReportRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ReviewReportRepository reviewReportRepository;

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

    // 해당 가게를 소유한 상인만 자기 가게 리뷰를 신고할 수 있음 (프론트 노출과 별개로 서버에서 재검증)
    @Transactional
    public void reportReview(ReviewReportRequestDto dto) {
        Review review = reviewRepository.findById(dto.getReviewIdx())
                .orElseThrow(() -> new IllegalStateException("신고할 리뷰를 찾을 수 없습니다."));

        User reporter = userRepository.findByUserIdx(dto.getReporterUserIdx())
                .orElseThrow(() -> new IllegalStateException("신고자 정보를 찾을 수 없습니다."));

        User owner = review.getRestaurant().getUser();
        if (owner == null || owner.getUserIdx() != reporter.getUserIdx()) {
            throw new SecurityException("해당 가게를 소유한 상인만 신고할 수 있습니다.");
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("신고 사유를 입력해야 합니다.");
        }

        ReviewReport report = new ReviewReport();
        report.setReportedReview(review);
        report.setReporter(reporter);
        report.setReason(dto.getReason().trim());
        report.setReportAt(LocalDateTime.now());
        report.setStatus("PENDING");

        reviewReportRepository.save(report);
    }

    @Transactional
    public Page<MyReviewResponseDto> getMyReviews(int userIdx, Pageable pageable) {
        return reviewRepository.findActiveByUserIdxOrderByReviewAtDesc(userIdx, pageable)
                .map(MyReviewResponseDto::new);
    }

    @Transactional
    public void updateMyReview(int reviewIdx, ReviewRequestDto dto) {
        Review review = reviewRepository.findById(reviewIdx)
                .orElseThrow(() -> new IllegalStateException("리뷰를 찾을 수 없습니다."));

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException("수정할 수 없는 리뷰입니다.");
        }
        if (review.getUser() == null || review.getUser().getUserIdx() != dto.getUserIdx()) {
            throw new SecurityException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("리뷰 내용을 입력해야 합니다.");
        }
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new IllegalArgumentException("별점은 1~5 사이여야 합니다.");
        }

        review.setContent(dto.getContent().trim());
        review.setRating(dto.getRating());
        review.setReviewUpdateAt(LocalDateTime.now());
    }

    @Transactional
    public void deleteMyReview(int reviewIdx, int userIdx) {
        Review review = reviewRepository.findById(reviewIdx)
                .orElseThrow(() -> new IllegalStateException("리뷰를 찾을 수 없습니다."));

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException("이미 삭제되었거나 차단된 리뷰입니다.");
        }
        if (review.getUser() == null || review.getUser().getUserIdx() != userIdx) {
            throw new SecurityException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }

        review.setStatus("DELETED");
        review.setReviewDeleteAt(LocalDateTime.now());
    }
}
