package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Coupon;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.domain.UserCoupon;
import com.hyeongju.crs.crs.dto.CouponRequestDto;
import com.hyeongju.crs.crs.dto.CouponResponseDto;
import com.hyeongju.crs.crs.dto.MyCouponResponseDto;
import com.hyeongju.crs.crs.repository.CouponRepository;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.UserCouponRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    // ===================== 상인 =====================

    // 쿠폰 등록 (본인 소유 가게인지 검증)
    @Transactional
    public Coupon createCoupon(CouponRequestDto dto) {
        Restaurant restaurant = restaurantRepository.findByRestIdx(dto.getRestIdx())
                .orElseThrow(() -> new IllegalStateException("가게를 찾을 수 없습니다."));

        if (restaurant.getUser() == null || restaurant.getUser().getUserIdx() != dto.getMerchantUserIdx()) {
            throw new SecurityException("본인 소유의 가게에만 쿠폰을 등록할 수 있습니다.");
        }
        if (dto.getPointCost() <= 0) {
            throw new IllegalArgumentException("필요 포인트는 1 이상이어야 합니다.");
        }

        Coupon coupon = new Coupon();
        coupon.setRestaurant(restaurant);
        coupon.setTitle(dto.getTitle());
        coupon.setDescription(dto.getDescription());
        coupon.setPointCost(dto.getPointCost());
        coupon.setValidUntil(dto.getValidUntil());
        coupon.setActive(true);
        return couponRepository.save(coupon);
    }

    // 내(상인) 가게들의 쿠폰 목록
    @Transactional
    public List<CouponResponseDto> getMyStoreCoupons(int merchantUserIdx) {
        return couponRepository.findByRestaurant_User_UserIdx(merchantUserIdx).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // 쿠폰 비활성화 (소유 검증)
    @Transactional
    public void deactivateCoupon(int couponIdx, int merchantUserIdx) {
        Coupon coupon = couponRepository.findById(couponIdx)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));
        Restaurant restaurant = coupon.getRestaurant();
        if (restaurant.getUser() == null || restaurant.getUser().getUserIdx() != merchantUserIdx) {
            throw new SecurityException("본인 쿠폰만 삭제할 수 있습니다.");
        }
        coupon.setActive(false);
    }

    // ===================== 유저 =====================

    // 교환 가능한 쿠폰 목록 (전체)
    @Transactional
    public List<CouponResponseDto> getAvailableCoupons() {
        return couponRepository.findAvailable(LocalDate.now()).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // 특정 가게의 교환 가능한 쿠폰 목록
    @Transactional
    public List<CouponResponseDto> getAvailableCouponsByRestIdx(int restIdx) {
        return couponRepository.findAvailableByRestIdx(restIdx, LocalDate.now()).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    // 포인트로 쿠폰 교환
    @Transactional
    public void redeemCoupon(int couponIdx, int userIdx) {
        User user = userRepository.findById(userIdx)
                .orElseThrow(() -> new IllegalStateException("유저를 찾을 수 없습니다."));
        Coupon coupon = couponRepository.findById(couponIdx)
                .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다."));

        if (!coupon.isActive()) {
            throw new IllegalStateException("현재 교환할 수 없는 쿠폰입니다.");
        }
        if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDate.now())) {
            throw new IllegalStateException("유효기간이 지난 쿠폰입니다.");
        }

        int balance = rewardService.getBalance(userIdx);
        if (balance < coupon.getPointCost()) {
            throw new IllegalArgumentException(
                    "포인트가 부족합니다. (보유 " + balance + "P / 필요 " + coupon.getPointCost() + "P)");
        }

        // 포인트 차감 + 쿠폰 발급 (같은 트랜잭션이라 하나라도 실패하면 함께 롤백)
        rewardService.spendPoints(user, coupon.getRestaurant(), coupon.getPointCost(),
                "쿠폰 교환 - " + coupon.getTitle());

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUser(user);
        userCoupon.setCoupon(coupon);
        userCoupon.setUsed(false);
        userCouponRepository.save(userCoupon);
    }

    // 보유 쿠폰 목록
    @Transactional
    public List<MyCouponResponseDto> getMyCoupons(int userIdx) {
        return userCouponRepository.findByUserUserIdxOrderByIssuedAtDesc(userIdx).stream()
                .map(this::toMyCouponDto)
                .collect(Collectors.toList());
    }

    // 쿠폰 사용 처리
    @Transactional
    public void useCoupon(int userCouponIdx, int userIdx) {
        UserCoupon userCoupon = userCouponRepository.findById(userCouponIdx)
                .orElseThrow(() -> new IllegalStateException("보유한 쿠폰을 찾을 수 없습니다."));
        if (userCoupon.getUser().getUserIdx() != userIdx) {
            throw new SecurityException("본인 쿠폰만 사용할 수 있습니다.");
        }
        if (userCoupon.isUsed()) {
            throw new IllegalStateException("이미 사용한 쿠폰입니다.");
        }
        userCoupon.setUsed(true);
        userCoupon.setUsedAt(LocalDateTime.now());
    }

    // ===================== 매퍼 =====================

    private CouponResponseDto toResponseDto(Coupon c) {
        return new CouponResponseDto(
                c.getCouponIdx(),
                c.getRestaurant().getRestIdx(),
                c.getRestaurant().getRestName(),
                c.getTitle(),
                c.getDescription(),
                c.getPointCost(),
                c.getValidUntil(),
                c.isActive());
    }

    private MyCouponResponseDto toMyCouponDto(UserCoupon uc) {
        Coupon c = uc.getCoupon();
        return new MyCouponResponseDto(
                uc.getUserCouponIdx(),
                c.getTitle(),
                c.getRestaurant().getRestName(),
                c.getPointCost(),
                c.getValidUntil(),
                uc.isUsed(),
                uc.getIssuedAt(),
                uc.getUsedAt());
    }
}
