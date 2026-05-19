package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.ReviewReportRequestDto;
import com.hyeongju.crs.crs.dto.ReviewRequestDto;
import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.service.ReviewService;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<String> registerReview(@RequestBody ReviewRequestDto requestDto){
        reviewService.saveReview(requestDto);

        return ResponseEntity.ok("리뷰가 등록되었습니다.");
    }

    @PostMapping("/report")
    public ResponseEntity<String> reportReview(@RequestBody ReviewReportRequestDto requestDto){
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

}
