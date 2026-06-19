package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.CouponRequestDto;
import com.hyeongju.crs.crs.dto.CouponResponseDto;
import com.hyeongju.crs.crs.dto.MyCouponResponseDto;
import com.hyeongju.crs.crs.service.CouponService;
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
    public ResponseEntity<String> register(@RequestBody CouponRequestDto dto) {
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
    public ResponseEntity<List<CouponResponseDto>> myStoreCoupons(@PathVariable("merchantUserIdx") int merchantUserIdx) {
        return ResponseEntity.ok(couponService.getMyStoreCoupons(merchantUserIdx));
    }

    // 쿠폰 비활성화
    @PostMapping("/delete/{couponIdx}")
    public ResponseEntity<String> delete(@PathVariable("couponIdx") int couponIdx,
                                         @RequestParam("merchantUserIdx") int merchantUserIdx) {
        try {
            couponService.deactivateCoupon(couponIdx, merchantUserIdx);
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
                                         @RequestParam("userIdx") int userIdx) {
        try {
            couponService.redeemCoupon(couponIdx, userIdx);
            return ResponseEntity.ok("쿠폰을 교환했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 보유 쿠폰 목록
    @GetMapping("/my/{userIdx}")
    public ResponseEntity<List<MyCouponResponseDto>> myCoupons(@PathVariable("userIdx") int userIdx) {
        return ResponseEntity.ok(couponService.getMyCoupons(userIdx));
    }

    // 쿠폰 사용
    @PostMapping("/use/{userCouponIdx}")
    public ResponseEntity<String> use(@PathVariable("userCouponIdx") int userCouponIdx,
                                      @RequestParam("userIdx") int userIdx) {
        try {
            couponService.useCoupon(userCouponIdx, userIdx);
            return ResponseEntity.ok("쿠폰을 사용했습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}
