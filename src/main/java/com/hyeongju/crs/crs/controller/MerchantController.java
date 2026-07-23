package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.MerchantUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.service.MerchantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/merchants")
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping("/mypage")
    public ResponseEntity<?> getMerchantProfile(HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 만료되었습니다.");
        try {
            MypageResponseDto dto = merchantService.getMerchantProfile(userIdx);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping("/mypage/updateMerchant")
    public ResponseEntity<?> updateMerchantProfile(@Valid @RequestBody MerchantUpdateDto dto, HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        merchantService.updateMerchantProfile(userIdx, dto);
        return ResponseEntity.ok("회원 정보가 수정되었습니다.");
    }
}
