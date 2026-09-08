package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.Review;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MyReviewResponseDto;
import com.hyeongju.crs.crs.dto.ReviewReportRequestDto;
import com.hyeongju.crs.crs.dto.ReviewRequestDto;
import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static com.hyeongju.crs.crs.controller.TestAuth.authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Review sampleReview() {
        User author = new User();
        author.setUserIdx(2);
        author.setName("작성자");

        Restaurant restaurant = new Restaurant();
        restaurant.setRestIdx(10);
        restaurant.setRestName("맛있는집");
        restaurant.setKakaoId("kakao-1");

        Review review = new Review();
        review.setReviewIdx(100);
        review.setContent("맛있어요");
        review.setRating(5);
        review.setReviewAt(LocalDateTime.now());
        review.setStatus("ACTIVE");
        review.setUser(author);
        review.setRestaurant(restaurant);
        return review;
    }

    private ReviewRequestDto requestDto() {
        ReviewRequestDto dto = new ReviewRequestDto();
        dto.setRestIdx(10);
        dto.setContent("정말 맛있어요");
        dto.setRating(4);
        return dto;
    }

    // ===================== 조회 =====================

    @Test
    @DisplayName("GET /api/reviews/{restIdx} - 리뷰 목록 조회 (비로그인 허용)")
    void getReviewByRestIdx() throws Exception {
        given(reviewService.getReviewsByRestaurant(10))
                .willReturn(List.of(new ReviewResponseDto(sampleReview())));

        mockMvc.perform(get("/api/reviews/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewIdx").value(100))
                .andExpect(jsonPath("$[0].content").value("맛있어요"))
                .andExpect(jsonPath("$[0].userName").value("작성자"));
    }

    // ===================== 등록 =====================

    @Test
    @DisplayName("POST /api/reviews/register - 등록 성공 시 200, 작성자는 토큰의 userIdx")
    void registerReview_success() throws Exception {
        mockMvc.perform(post("/api/reviews/register")
                        .with(authentication(new TestingAuthenticationToken(2, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("리뷰가 등록되었습니다."));

        org.mockito.ArgumentCaptor<ReviewRequestDto> captor =
                org.mockito.ArgumentCaptor.forClass(ReviewRequestDto.class);
        verify(reviewService).saveReview(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUserIdx()).isEqualTo(2);
    }

    @Test
    @DisplayName("POST /api/reviews/register - 인증 정보가 없으면 401")
    void registerReview_unauthorized() throws Exception {
        mockMvc.perform(post("/api/reviews/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));

        verify(reviewService, never()).saveReview(any(ReviewRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/reviews/register - 내용이 비어있으면 400")
    void registerReview_blankContent() throws Exception {
        ReviewRequestDto dto = requestDto();
        dto.setContent("");

        mockMvc.perform(post("/api/reviews/register")
                        .with(authentication(new TestingAuthenticationToken(2, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews/register - 별점이 범위를 벗어나면 400")
    void registerReview_invalidRating() throws Exception {
        ReviewRequestDto dto = requestDto();
        dto.setRating(6);

        mockMvc.perform(post("/api/reviews/register")
                        .with(authentication(new TestingAuthenticationToken(2, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).saveReview(any(ReviewRequestDto.class));
    }

    // ===================== 신고 =====================

    private ReviewReportRequestDto reportDto() {
        ReviewReportRequestDto dto = new ReviewReportRequestDto();
        dto.setReviewIdx(100);
        dto.setReason("욕설");
        return dto;
    }

    @Test
    @DisplayName("POST /api/reviews/report - 신고 성공 시 200")
    void reportReview_success() throws Exception {
        mockMvc.perform(post("/api/reviews/report")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("리뷰가 신고되었습니다."));
    }

    @Test
    @DisplayName("POST /api/reviews/report - 인증 정보가 없으면 401")
    void reportReview_unauthorized() throws Exception {
        mockMvc.perform(post("/api/reviews/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDto())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/reviews/report - 가게 소유 상인이 아니면 403")
    void reportReview_forbidden() throws Exception {
        willThrow(new SecurityException("해당 가게를 소유한 상인만 신고할 수 있습니다."))
                .given(reviewService).reportReview(any(ReviewReportRequestDto.class));

        mockMvc.perform(post("/api/reviews/report")
                        .with(authentication(new TestingAuthenticationToken(2, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/reviews/report - 신고 사유가 없으면 400")
    void reportReview_badRequest() throws Exception {
        willThrow(new IllegalArgumentException("신고 사유를 입력해야 합니다."))
                .given(reviewService).reportReview(any(ReviewReportRequestDto.class));

        mockMvc.perform(post("/api/reviews/report")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDto())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reviews/report - 리뷰가 없으면 404")
    void reportReview_notFound() throws Exception {
        willThrow(new IllegalStateException("신고할 리뷰를 찾을 수 없습니다."))
                .given(reviewService).reportReview(any(ReviewReportRequestDto.class));

        mockMvc.perform(post("/api/reviews/report")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportDto())))
                .andExpect(status().isNotFound());
    }

    // ===================== 내 리뷰 =====================

    @Test
    @DisplayName("GET /api/reviews/my/{userIdx} - 내 리뷰 페이징 조회")
    void getMyReviews_success() throws Exception {
        Pageable pageable = PageRequest.of(0, 2);
        given(reviewService.getMyReviews(eq(2), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(new MyReviewResponseDto(sampleReview())), pageable, 1));

        mockMvc.perform(get("/api/reviews/my/2").with(authentication(new TestingAuthenticationToken(2, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewIdx").value(100))
                .andExpect(jsonPath("$.content[0].restName").value("맛있는집"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/reviews/my/{userIdx} - 인증 정보가 없으면 401")
    void getMyReviews_unauthorized() throws Exception {
        mockMvc.perform(get("/api/reviews/my/2"))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).getMyReviews(anyInt(), any(Pageable.class));
    }

    @Test
    @DisplayName("PUT /api/reviews/{reviewIdx} - 수정 성공 시 200")
    void updateMyReview_success() throws Exception {
        mockMvc.perform(put("/api/reviews/100")
                        .with(authentication(new TestingAuthenticationToken(2, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("리뷰가 수정되었습니다."));

        verify(reviewService).updateMyReview(eq(100), any(ReviewRequestDto.class));
    }

    @Test
    @DisplayName("PUT /api/reviews/{reviewIdx} - 인증 정보가 없으면 401")
    void updateMyReview_unauthorized() throws Exception {
        mockMvc.perform(put("/api/reviews/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/reviews/{reviewIdx} - 본인 리뷰가 아니면 403")
    void updateMyReview_forbidden() throws Exception {
        willThrow(new SecurityException("본인이 작성한 리뷰만 수정할 수 있습니다."))
                .given(reviewService).updateMyReview(anyInt(), any(ReviewRequestDto.class));

        mockMvc.perform(put("/api/reviews/100")
                        .with(authentication(new TestingAuthenticationToken(999, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/reviews/{reviewIdx} - 리뷰가 없으면 404")
    void updateMyReview_notFound() throws Exception {
        willThrow(new IllegalStateException("리뷰를 찾을 수 없습니다."))
                .given(reviewService).updateMyReview(anyInt(), any(ReviewRequestDto.class));

        mockMvc.perform(put("/api/reviews/999")
                        .with(authentication(new TestingAuthenticationToken(2, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewIdx} - 삭제 성공 시 200")
    void deleteMyReview_success() throws Exception {
        mockMvc.perform(delete("/api/reviews/100")
                        .param("userIdx", "2")
                        .with(authentication(new TestingAuthenticationToken(2, null))))
                .andExpect(status().isOk())
                .andExpect(content().string("리뷰가 삭제되었습니다."));

        verify(reviewService).deleteMyReview(100, 2);
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewIdx} - 인증 정보가 없으면 401")
    void deleteMyReview_unauthorized() throws Exception {
        mockMvc.perform(delete("/api/reviews/100").param("userIdx", "2"))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).deleteMyReview(anyInt(), anyInt());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewIdx} - 본인 리뷰가 아니면 403")
    void deleteMyReview_forbidden() throws Exception {
        willThrow(new SecurityException("본인이 작성한 리뷰만 삭제할 수 있습니다."))
                .given(reviewService).deleteMyReview(anyInt(), anyInt());

        mockMvc.perform(delete("/api/reviews/100")
                        .param("userIdx", "2")
                        .with(authentication(new TestingAuthenticationToken(999, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/reviews/{reviewIdx} - 이미 삭제된 리뷰면 404")
    void deleteMyReview_notFound() throws Exception {
        willThrow(new IllegalStateException("이미 삭제되었거나 차단된 리뷰입니다."))
                .given(reviewService).deleteMyReview(anyInt(), anyInt());

        mockMvc.perform(delete("/api/reviews/100")
                        .param("userIdx", "2")
                        .with(authentication(new TestingAuthenticationToken(2, null))))
                .andExpect(status().isNotFound());
    }
}
