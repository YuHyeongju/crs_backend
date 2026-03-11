package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.ReviewResponseDto;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
