package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.CouponRequestDto;
import com.hyeongju.crs.crs.dto.CouponResponseDto;
import com.hyeongju.crs.crs.dto.MyCouponResponseDto;
import com.hyeongju.crs.crs.service.CouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ===================== 상인 =====================

    // 쿠폰 등록
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody CouponRequestDto dto, HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        dto.setMerchantUserIdx(authedUserIdx);
        try {
            couponService.createCoupon(dto);
            return ResponseEntity.ok("쿠폰이 등록되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 내 가게 쿠폰 목록
    @GetMapping("/my-store/{merchantUserIdx}")
    public ResponseEntity<List<CouponResponseDto>> myStoreCoupons(@PathVariable("merchantUserIdx") int merchantUserIdx,
                                                                   HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(couponService.getMyStoreCoupons(authedUserIdx));
    }

    // 쿠폰 수정
    @PostMapping("/update/{couponIdx}")
    public ResponseEntity<String> update(@PathVariable("couponIdx") int couponIdx,
                                         @RequestParam("merchantUserIdx") int merchantUserIdx,
                                         @Valid @RequestBody CouponRequestDto dto,
                                         HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            couponService.updateCoupon(couponIdx, authedUserIdx, dto);
            return ResponseEntity.ok("쿠폰이 수정되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 쿠폰 비활성화
    @PostMapping("/delete/{couponIdx}")
    public ResponseEntity<String> delete(@PathVariable("couponIdx") int couponIdx,
                                         @RequestParam("merchantUserIdx") int merchantUserIdx,
                                         HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            couponService.deactivateCoupon(couponIdx, authedUserIdx);
            return ResponseEntity.ok("쿠폰이 삭제되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ===================== 유저 =====================

    // 교환 가능 쿠폰 목록 (전체)
    @GetMapping("/available")
    public ResponseEntity<List<CouponResponseDto>> available() {
        return ResponseEntity.ok(couponService.getAvailableCoupons());
    }

    // 특정 가게의 교환 가능 쿠폰 목록
    @GetMapping("/available/restaurant/{restIdx}")
    public ResponseEntity<List<CouponResponseDto>> availableByRestaurant(@PathVariable("restIdx") int restIdx) {
        return ResponseEntity.ok(couponService.getAvailableCouponsByRestIdx(restIdx));
    }

    // 포인트로 쿠폰 교환
    @PostMapping("/{couponIdx}/redeem")
    public ResponseEntity<String> redeem(@PathVariable("couponIdx") int couponIdx,
                                         @RequestParam("userIdx") int userIdx,
                                         HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            couponService.redeemCoupon(couponIdx, authedUserIdx);
            return ResponseEntity.ok("쿠폰을 교환했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 보유 쿠폰 목록
    @GetMapping("/my/{userIdx}")
    public ResponseEntity<List<MyCouponResponseDto>> myCoupons(@PathVariable("userIdx") int userIdx,
                                                                HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(couponService.getMyCoupons(authedUserIdx));
    }

    // 쿠폰 사용
    @PostMapping("/use/{userCouponIdx}")
    public ResponseEntity<String> use(@PathVariable("userCouponIdx") int userCouponIdx,
                                      @RequestParam("userIdx") int userIdx,
                                      HttpServletRequest request) {
        Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (authedUserIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            couponService.useCoupon(userCouponIdx, authedUserIdx);
            return ResponseEntity.ok("쿠폰을 사용했습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
