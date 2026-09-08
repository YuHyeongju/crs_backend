package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.RewardBalanceResponseDto;
import com.hyeongju.crs.crs.service.RewardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {
    // 유저 포인트 잔액 조회 API

    private final RewardService rewardService;

    @GetMapping("/balance/{userIdx}")
    public ResponseEntity<RewardBalanceResponseDto> getBalance(@PathVariable("userIdx") int userIdx,
                                                                Authentication authentication) {
        // 로그인한 유저 본인의 포인트 잔액 조회 API
        Integer authedUserIdx = authentication != null ? (Integer) authentication.getPrincipal() : null;
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 경로변수가 아닌 토큰의 authedUserIdx 기준으로 조회 — 남의 잔액 조회 방지
        int balance = rewardService.getBalance(authedUserIdx);
        return ResponseEntity.ok(new RewardBalanceResponseDto(balance));
    }
}
