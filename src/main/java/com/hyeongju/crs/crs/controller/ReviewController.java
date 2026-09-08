package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.MyReviewResponseDto;
import com.hyeongju.crs.crs.dto.ReviewReportRequestDto;
import com.hyeongju.crs.crs.dto.ReviewRequestDto;
import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 리뷰 API: 조회/등록/수정/삭제 + 리뷰 신고
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // 특정 가게의 리뷰 목록 조회
    @GetMapping("/{restIdx}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewByRestIdx(@PathVariable("restIdx") int restIdx){
        List<ReviewResponseDto> reviews = reviewService.getReviewsByRestaurant(restIdx);
        return ResponseEntity.ok(reviews);
    }

    // 리뷰 등록(작성자는 요청 바디가 아니라 JWT 인증 정보로 지정)
    @PostMapping("/register")
    public ResponseEntity<String> registerReview(@Valid @RequestBody ReviewRequestDto requestDto, Authentication authentication){
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        requestDto.setUserIdx(authedUserIdx);
        reviewService.saveReview(requestDto);

        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    // 리뷰 신고(신고자도 JWT 인증 정보에서 추출)
    @PostMapping("/report")
    public ResponseEntity<String> reportReview(@RequestBody ReviewReportRequestDto requestDto, Authentication authentication){
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        requestDto.setReporterUserIdx(authedUserIdx);
        try {
            reviewService.reportReview(requestDto);
            return ResponseEntity.ok("리뷰가 신고되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 내가 쓴 리뷰 목록 페이지 단위 조회(기본 2개씩, 최신순)
    @GetMapping("/my/{userIdx}")
    public ResponseEntity<Page<MyReviewResponseDto>> getMyReviews(
            @PathVariable("userIdx") int userIdx,
            @PageableDefault(size = 2, sort = "reviewAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reviewService.getMyReviews(authedUserIdx, pageable));
    }

    // 내 리뷰 수정(작성자 본인 확인은 서비스단에서 처리)
    @PutMapping("/{reviewIdx}")
    public ResponseEntity<String> updateMyReview(@PathVariable("reviewIdx") int reviewIdx,
                                                 @Valid @RequestBody ReviewRequestDto requestDto,
                                                 Authentication authentication) {
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        requestDto.setUserIdx(authedUserIdx);
        try {
            reviewService.updateMyReview(reviewIdx, requestDto);
            return ResponseEntity.ok("리뷰가 수정되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 내 리뷰 삭제
    @DeleteMapping("/{reviewIdx}")
    public ResponseEntity<String> deleteMyReview(@PathVariable("reviewIdx") int reviewIdx,
                                                 @RequestParam("userIdx") int userIdx,
                                                 Authentication authentication) {
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            reviewService.deleteMyReview(reviewIdx, authedUserIdx);
            return ResponseEntity.ok("리뷰가 삭제되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
