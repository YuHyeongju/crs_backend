package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.RewardBalanceResponseDto;
import com.hyeongju.crs.crs.service.RewardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards")
@RequiredArgsConstructor
public class RewardController {

    private final RewardService rewardService;

    // 유저 보유 포인트 조회 (마이페이지 리워드 패널용)
    @GetMapping("/balance/{userIdx}")
    public ResponseEntity<RewardBalanceResponseDto> getBalance(@PathVariable("userIdx") int userIdx,
                                                                HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int balance = rewardService.getBalance(authedUserIdx);
        return ResponseEntity.ok(new RewardBalanceResponseDto(balance));
    }
}
