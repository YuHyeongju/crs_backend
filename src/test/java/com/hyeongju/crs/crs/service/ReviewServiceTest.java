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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User author;
    private User merchant;
    private Restaurant restaurant;
    private Review review;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setUserIdx(2);
        author.setName("작성자");

        merchant = new User();
        merchant.setUserIdx(1);
        merchant.setName("사장님");

        restaurant = new Restaurant();
        restaurant.setRestIdx(10);
        restaurant.setRestName("맛있는집");
        restaurant.setKakaoId("kakao-1");
        restaurant.setUser(merchant);

        review = new Review();
        review.setReviewIdx(100);
        review.setContent("맛있어요");
        review.setRating(5);
        review.setReviewAt(LocalDateTime.now());
        review.setStatus("ACTIVE");
        review.setUser(author);
        review.setRestaurant(restaurant);
    }

    private ReviewRequestDto requestDto(String content, int rating, int userIdx) {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRestIdx(10);
        dto.setContent(content);
        dto.setRating(rating);
        dto.setUserIdx(userIdx);
        return dto;
    }

    // ===================== 조회 =====================

    @Test
    @DisplayName("가게별 리뷰 목록 조회")
    void getReviewsByRestaurant() {
        given(reviewRepository.findByRestaurant_RestIdxOrderByReviewAtDesc(10)).willReturn(List.of(review));

        List<ReviewResponseDto> result = reviewService.getReviewsByRestaurant(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReviewIdx()).isEqualTo(100);
        assertThat(result.get(0).getContent()).isEqualTo("맛있어요");
        assertThat(result.get(0).getRating()).isEqualTo(5);
        assertThat(result.get(0).getUserIdx()).isEqualTo(2);
        assertThat(result.get(0).getUserName()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("리뷰가 없으면 빈 목록")
    void getReviewsByRestaurant_empty() {
        given(reviewRepository.findByRestaurant_RestIdxOrderByReviewAtDesc(10)).willReturn(List.of());

        assertThat(reviewService.getReviewsByRestaurant(10)).isEmpty();
    }

    // ===================== 등록 =====================

    @Test
    @DisplayName("리뷰 등록 성공")
    void saveReview_success() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(userRepository.findByUserIdx(2)).willReturn(Optional.of(author));

        reviewService.saveReview(requestDto("정말 맛있어요", 4, 2));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertThat(saved.getContent()).isEqualTo("정말 맛있어요");
        assertThat(saved.getRating()).isEqualTo(4);
        assertThat(saved.getUser()).isSameAs(author);
        assertThat(saved.getRestaurant()).isSameAs(restaurant);
        assertThat(saved.getReviewAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("가게가 없으면 리뷰 등록 실패")
    void saveReview_restaurantNotFound() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.saveReview(requestDto("내용", 4, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("가게를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("유저가 없으면 리뷰 등록 실패")
    void saveReview_userNotFound() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(userRepository.findByUserIdx(2)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.saveReview(requestDto("내용", 4, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("유저를 찾을 수 없습니다.");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    // ===================== 신고 =====================

    private ReviewReportRequestDto reportDto(int reviewIdx, int reporterIdx, String reason) {
        ReviewReportRequestDto dto = new ReviewReportRequestDto();
        dto.setReviewIdx(reviewIdx);
        dto.setReporterUserIdx(reporterIdx);
        dto.setReason(reason);
        return dto;
    }

    @Test
    @DisplayName("가게 소유 상인이 리뷰를 신고하면 PENDING 상태로 저장된다")
    void reportReview_success() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));

        reviewService.reportReview(reportDto(100, 1, "  욕설이 포함되어 있습니다  "));

        ArgumentCaptor<ReviewReport> captor = ArgumentCaptor.forClass(ReviewReport.class);
        verify(reviewReportRepository).save(captor.capture());
        ReviewReport saved = captor.getValue();
        assertThat(saved.getReason()).isEqualTo("욕설이 포함되어 있습니다");
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getReporter()).isSameAs(merchant);
        assertThat(saved.getReportedReview()).isSameAs(review);
        assertThat(saved.getReportAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 신고 시 IllegalStateException")
    void reportReview_reviewNotFound() {
        given(reviewRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.reportReview(reportDto(999, 1, "사유")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("신고할 리뷰를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("신고자 정보를 찾을 수 없으면 IllegalStateException")
    void reportReview_reporterNotFound() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.reportReview(reportDto(100, 99, "사유")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("신고자 정보를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("가게 소유 상인이 아니면 신고 시 SecurityException")
    void reportReview_notOwner() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));
        given(userRepository.findByUserIdx(2)).willReturn(Optional.of(author));

        assertThatThrownBy(() -> reviewService.reportReview(reportDto(100, 2, "사유")))
                .isInstanceOf(SecurityException.class)
                .hasMessage("해당 가게를 소유한 상인만 신고할 수 있습니다.");
    }

    @Test
    @DisplayName("소유 상인이 없는 가게의 리뷰는 신고할 수 없다")
    void reportReview_ownerlessRestaurant() {
        restaurant.setUser(null);
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));

        assertThatThrownBy(() -> reviewService.reportReview(reportDto(100, 1, "사유")))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("신고 사유가 비어있으면 IllegalArgumentException")
    void reportReview_blankReason() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));

        assertThatThrownBy(() -> reviewService.reportReview(reportDto(100, 1, "   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("신고 사유를 입력해야 합니다.");
        verify(reviewReportRepository, never()).save(any(ReviewReport.class));
    }

    @Test
    @DisplayName("신고 사유가 null 이면 IllegalArgumentException")
    void reportReview_nullReason() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchant));

        assertThatThrownBy(() -> reviewService.reportReview(reportDto(100, 1, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===================== 내 리뷰 =====================

    @Test
    @DisplayName("내 리뷰 페이징 조회")
    void getMyReviews() {
        Pageable pageable = PageRequest.of(0, 2);
        given(reviewRepository.findActiveByUserIdxOrderByReviewAtDesc(eq(2), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(review), pageable, 1));

        Page<MyReviewResponseDto> result = reviewService.getMyReviews(2, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        MyReviewResponseDto dto = result.getContent().get(0);
        assertThat(dto.getReviewIdx()).isEqualTo(100);
        assertThat(dto.getRestIdx()).isEqualTo(10);
        assertThat(dto.getRestName()).isEqualTo("맛있는집");
        assertThat(dto.getKakaoId()).isEqualTo("kakao-1");
        assertThat(dto.getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("내 리뷰 수정 성공")
    void updateMyReview_success() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        reviewService.updateMyReview(100, requestDto("  수정된 내용  ", 3, 2));

        assertThat(review.getContent()).isEqualTo("수정된 내용");
        assertThat(review.getRating()).isEqualTo(3);
        assertThat(review.getReviewUpdateAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 수정 시 IllegalStateException")
    void updateMyReview_notFound() {
        given(reviewRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateMyReview(999, requestDto("내용", 3, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("리뷰를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 리뷰는 수정할 수 없다")
    void updateMyReview_notActive() {
        review.setStatus("BLOCKED");
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateMyReview(100, requestDto("내용", 3, 2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("수정할 수 없는 리뷰입니다.");
    }

    @Test
    @DisplayName("본인 리뷰가 아니면 수정 시 SecurityException")
    void updateMyReview_notOwner() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateMyReview(100, requestDto("내용", 3, 999)))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인이 작성한 리뷰만 수정할 수 있습니다.");
    }

    @Test
    @DisplayName("리뷰 내용이 비어있으면 IllegalArgumentException")
    void updateMyReview_blankContent() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateMyReview(100, requestDto("   ", 3, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("리뷰 내용을 입력해야 합니다.");
    }

    @Test
    @DisplayName("별점이 범위를 벗어나면 IllegalArgumentException")
    void updateMyReview_invalidRating() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateMyReview(100, requestDto("내용", 6, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("별점은 1~5 사이여야 합니다.");
    }

    @Test
    @DisplayName("별점이 0이면 IllegalArgumentException")
    void updateMyReview_zeroRating() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.updateMyReview(100, requestDto("내용", 0, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("내 리뷰 삭제 - 상태만 DELETED 로 바뀌는 소프트 삭제")
    void deleteMyReview_success() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        reviewService.deleteMyReview(100, 2);

        assertThat(review.getStatus()).isEqualTo("DELETED");
        assertThat(review.getReviewDeleteAt()).isNotNull();
        verify(reviewRepository, never()).delete(any(Review.class));
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 삭제 시 IllegalStateException")
    void deleteMyReview_notFound() {
        given(reviewRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteMyReview(999, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("리뷰를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 삭제된 리뷰는 다시 삭제할 수 없다")
    void deleteMyReview_alreadyDeleted() {
        review.setStatus("DELETED");
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteMyReview(100, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 삭제되었거나 차단된 리뷰입니다.");
    }

    @Test
    @DisplayName("본인 리뷰가 아니면 삭제 시 SecurityException")
    void deleteMyReview_notOwner() {
        given(reviewRepository.findById(100)).willReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.deleteMyReview(100, 999))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인이 작성한 리뷰만 삭제할 수 있습니다.");
    }
}
