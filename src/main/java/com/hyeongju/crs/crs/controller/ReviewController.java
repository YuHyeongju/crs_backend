package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.MyReviewResponseDto;
import com.hyeongju.crs.crs.dto.ReviewReportRequestDto;
import com.hyeongju.crs.crs.dto.ReviewRequestDto;
import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{restIdx}")
    public ResponseEntity<List<ReviewResponseDto>> getReviewByRestIdx(@PathVariable("restIdx") int restIdx){
        List<ReviewResponseDto> reviews = reviewService.getReviewsByRestaurant(restIdx);
        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerReview(@Valid @RequestBody ReviewRequestDto requestDto, HttpServletRequest request){
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        requestDto.setUserIdx(authedUserIdx);
        reviewService.saveReview(requestDto);

        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    @PostMapping("/report")
    public ResponseEntity<String> reportReview(@RequestBody ReviewReportRequestDto requestDto, HttpServletRequest request){
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
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

    @GetMapping("/my/{userIdx}")
    public ResponseEntity<Page<MyReviewResponseDto>> getMyReviews(
            @PathVariable("userIdx") int userIdx,
            @PageableDefault(size = 2, sort = "reviewAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(reviewService.getMyReviews(authedUserIdx, pageable));
    }

    @PutMapping("/{reviewIdx}")
    public ResponseEntity<String> updateMyReview(@PathVariable("reviewIdx") int reviewIdx,
                                                 @Valid @RequestBody ReviewRequestDto requestDto,
                                                 HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
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

    @DeleteMapping("/{reviewIdx}")
    public ResponseEntity<String> deleteMyReview(@PathVariable("reviewIdx") int reviewIdx,
                                                 @RequestParam("userIdx") int userIdx,
                                                 HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
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
