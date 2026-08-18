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
    // 리뷰 작성/조회/수정/삭제 및 리뷰 신고 처리를 담당하는 서비스

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final ReviewReportRepository reviewReportRepository;

    @Transactional
    public List<ReviewResponseDto> getReviewsByRestaurant(int restIdx){
        // 특정 식당의 ACTIVE 리뷰를 최신순으로 조회해서 응답 DTO 리스트로 변환
        List<Review> reviews = reviewRepository.findByRestaurant_RestIdxOrderByReviewAtDesc(restIdx);
        System.out.println("리뷰 가져오기 성공");
        System.out.println("가져온 리뷰 개수: " +  reviews.size() + "개");

        return reviews.stream().map(ReviewResponseDto :: new).collect(Collectors.toList());
    }
    @Transactional
    public void saveReview(ReviewRequestDto reviewRequestDto){
        // 리뷰 작성: 대상 식당/작성자 조회 → Review 엔티티 생성 → 저장

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

        User owner = review.getRestaurant().getUser(); // 리뷰가 달린 식당의 소유 상인
        if (owner == null || owner.getUserIdx() != reporter.getUserIdx()) {
            throw new SecurityException("해당 가게를 소유한 상인만 신고할 수 있습니다."); // 소유자가 아니면 신고 자체를 거부
        }

        if (dto.getReason() == null || dto.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("신고 사유를 입력해야 합니다.");
        }

        ReviewReport report = new ReviewReport();
        report.setReportedReview(review);
        report.setReporter(reporter);
        report.setReason(dto.getReason().trim());
        report.setReportAt(LocalDateTime.now());
        report.setStatus("PENDING"); // 신규 신고는 항상 처리 대기(PENDING) 상태로 시작

        reviewReportRepository.save(report);
    }

    @Transactional
    public Page<MyReviewResponseDto> getMyReviews(int userIdx, Pageable pageable) {
        // 마이페이지 "내가 쓴 리뷰"를 페이징 조회. Page<Review>를 Page<MyReviewResponseDto>로 매핑해서 반환
        return reviewRepository.findActiveByUserIdxOrderByReviewAtDesc(userIdx, pageable)
                .map(MyReviewResponseDto::new);
    }

    @Transactional
    public void updateMyReview(int reviewIdx, ReviewRequestDto dto) {
        // 본인 리뷰 수정: 상태/소유자/내용/별점 검증을 모두 통과해야 수정 가능
        Review review = reviewRepository.findById(reviewIdx)
                .orElseThrow(() -> new IllegalStateException("리뷰를 찾을 수 없습니다."));

        if (!"ACTIVE".equals(review.getStatus())) {
            throw new IllegalStateException("수정할 수 없는 리뷰입니다."); // 삭제/차단된 리뷰는 수정 불가
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
        // 여기도 save() 호출 없이 dirty checking으로 UPDATE가 자동 반영됨(@Transactional 범위 안이기 때문)
    }

    @Transactional
    public void deleteMyReview(int reviewIdx, int userIdx) {
        // 본인 리뷰 삭제(실제 DELETE가 아니라 상태값만 DELETED로 바꾸는 소프트 삭제)
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
