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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RewardService rewardService;

    @InjectMocks
    private CouponService couponService;

    private User merchant;
    private User customer;
    private Restaurant restaurant;
    private Coupon coupon;

    @BeforeEach
    void setUp() {
        merchant = new User();
        merchant.setUserIdx(1);
        merchant.setName("사장님");

        customer = new User();
        customer.setUserIdx(2);
        customer.setName("고객");

        restaurant = new Restaurant();
        restaurant.setRestIdx(10);
        restaurant.setRestName("맛있는집");
        restaurant.setUser(merchant);

        coupon = new Coupon();
        coupon.setCouponIdx(100);
        coupon.setRestaurant(restaurant);
        coupon.setTitle("아메리카노 무료");
        coupon.setDescription("설명");
        coupon.setPointCost(300);
        coupon.setActive(true);
    }

    private CouponRequestDto requestDto() {
        CouponRequestDto dto = new CouponRequestDto();
        dto.setMerchantUserIdx(1);
        dto.setRestIdx(10);
        dto.setTitle("아메리카노 무료");
        dto.setDescription("설명");
        dto.setPointCost(300);
        dto.setValidUntil(LocalDate.now().plusDays(30));
        return dto;
    }

    // ===================== 상인: 등록 =====================

    @Test
    @DisplayName("쿠폰 등록 성공")
    void createCoupon_success() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));
        given(couponRepository.save(any(Coupon.class))).willAnswer(inv -> inv.getArgument(0));

        Coupon saved = couponService.createCoupon(requestDto());

        assertThat(saved.getTitle()).isEqualTo("아메리카노 무료");
        assertThat(saved.getPointCost()).isEqualTo(300);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getRestaurant()).isSameAs(restaurant);
    }

    @Test
    @DisplayName("가게를 찾을 수 없으면 IllegalStateException")
    void createCoupon_restaurantNotFound() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.createCoupon(requestDto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("가게를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인 소유 가게가 아니면 SecurityException")
    void createCoupon_notOwner() {
        restaurant.setUser(customer); // 다른 사람 소유
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> couponService.createCoupon(requestDto()))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인 소유의 가게에만 쿠폰을 등록할 수 있습니다.");
    }

    @Test
    @DisplayName("소유자가 없는(카카오 자동생성) 가게에는 쿠폰 등록 불가")
    void createCoupon_ownerless() {
        restaurant.setUser(null);
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        assertThatThrownBy(() -> couponService.createCoupon(requestDto()))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("필요 포인트가 0 이하면 IllegalArgumentException")
    void createCoupon_invalidPointCost() {
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(restaurant));

        CouponRequestDto dto = requestDto();
        dto.setPointCost(0);

        assertThatThrownBy(() -> couponService.createCoupon(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필요 포인트는 1 이상이어야 합니다.");
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    // ===================== 상인: 목록/수정/삭제 =====================

    @Test
    @DisplayName("내 가게 쿠폰 목록 조회")
    void getMyStoreCoupons() {
        given(couponRepository.findByRestaurant_User_UserIdx(1)).willReturn(List.of(coupon));

        List<CouponResponseDto> result = couponService.getMyStoreCoupons(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCouponIdx()).isEqualTo(100);
        assertThat(result.get(0).getRestIdx()).isEqualTo(10);
        assertThat(result.get(0).getRestName()).isEqualTo("맛있는집");
        assertThat(result.get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("쿠폰 수정 성공")
    void updateCoupon_success() {
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        CouponRequestDto dto = requestDto();
        dto.setTitle("변경된 제목");
        dto.setPointCost(500);
        dto.setValidUntil(LocalDate.now().plusDays(10));

        couponService.updateCoupon(100, 1, dto);

        assertThat(coupon.getTitle()).isEqualTo("변경된 제목");
        assertThat(coupon.getPointCost()).isEqualTo(500);
        assertThat(coupon.getValidUntil()).isEqualTo(LocalDate.now().plusDays(10));
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 수정 시 IllegalStateException")
    void updateCoupon_notFound() {
        given(couponRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.updateCoupon(999, 1, requestDto()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인 쿠폰이 아니면 수정 시 SecurityException")
    void updateCoupon_notOwner() {
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.updateCoupon(100, 999, requestDto()))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인 쿠폰만 수정할 수 있습니다.");
    }

    @Test
    @DisplayName("수정 시 필요 포인트가 0 이하면 IllegalArgumentException")
    void updateCoupon_invalidPointCost() {
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        CouponRequestDto dto = requestDto();
        dto.setPointCost(-1);

        assertThatThrownBy(() -> couponService.updateCoupon(100, 1, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("필요 포인트는 1 이상이어야 합니다.");
    }

    @Test
    @DisplayName("쿠폰 비활성화 성공 (소프트 삭제)")
    void deactivateCoupon_success() {
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        couponService.deactivateCoupon(100, 1);

        assertThat(coupon.isActive()).isFalse();
    }

    @Test
    @DisplayName("본인 쿠폰이 아니면 비활성화 시 SecurityException")
    void deactivateCoupon_notOwner() {
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.deactivateCoupon(100, 999))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인 쿠폰만 삭제할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 비활성화 시 IllegalStateException")
    void deactivateCoupon_notFound() {
        given(couponRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.deactivateCoupon(999, 1))
                .isInstanceOf(IllegalStateException.class);
    }

    // ===================== 유저: 조회 =====================

    @Test
    @DisplayName("교환 가능 쿠폰 전체 목록 조회")
    void getAvailableCoupons() {
        given(couponRepository.findAvailable(any(LocalDate.class))).willReturn(List.of(coupon));

        List<CouponResponseDto> result = couponService.getAvailableCoupons();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("아메리카노 무료");
    }

    @Test
    @DisplayName("특정 가게의 교환 가능 쿠폰 목록 조회")
    void getAvailableCouponsByRestIdx() {
        given(couponRepository.findAvailableByRestIdx(eq(10), any(LocalDate.class)))
                .willReturn(List.of(coupon));

        List<CouponResponseDto> result = couponService.getAvailableCouponsByRestIdx(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestIdx()).isEqualTo(10);
    }

    // ===================== 유저: 교환 =====================

    @Test
    @DisplayName("포인트로 쿠폰 교환 성공 - 포인트 차감 후 보유 쿠폰이 생성된다")
    void redeemCoupon_success() {
        given(userRepository.findById(2)).willReturn(Optional.of(customer));
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));
        given(rewardService.getBalance(2)).willReturn(1000);

        couponService.redeemCoupon(100, 2);

        verify(rewardService).spendPoints(customer, restaurant, 300, "쿠폰 교환 - 아메리카노 무료");

        ArgumentCaptor<UserCoupon> captor = ArgumentCaptor.forClass(UserCoupon.class);
        verify(userCouponRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(customer);
        assertThat(captor.getValue().getCoupon()).isSameAs(coupon);
        assertThat(captor.getValue().isUsed()).isFalse();
    }

    @Test
    @DisplayName("유저를 찾을 수 없으면 IllegalStateException")
    void redeemCoupon_userNotFound() {
        given(userRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.redeemCoupon(100, 99))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("유저를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("쿠폰을 찾을 수 없으면 IllegalStateException")
    void redeemCoupon_couponNotFound() {
        given(userRepository.findById(2)).willReturn(Optional.of(customer));
        given(couponRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.redeemCoupon(999, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("쿠폰을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("비활성 쿠폰은 교환할 수 없다")
    void redeemCoupon_inactive() {
        coupon.setActive(false);
        given(userRepository.findById(2)).willReturn(Optional.of(customer));
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.redeemCoupon(100, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("현재 교환할 수 없는 쿠폰입니다.");
    }

    @Test
    @DisplayName("유효기간이 지난 쿠폰은 교환할 수 없다")
    void redeemCoupon_expired() {
        coupon.setValidUntil(LocalDate.now().minusDays(1));
        given(userRepository.findById(2)).willReturn(Optional.of(customer));
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.redeemCoupon(100, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("유효기간이 지난 쿠폰입니다.");
    }

    @Test
    @DisplayName("포인트가 부족하면 IllegalArgumentException")
    void redeemCoupon_insufficientPoints() {
        given(userRepository.findById(2)).willReturn(Optional.of(customer));
        given(couponRepository.findById(100)).willReturn(Optional.of(coupon));
        given(rewardService.getBalance(2)).willReturn(100);

        assertThatThrownBy(() -> couponService.redeemCoupon(100, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트가 부족합니다.");
        verify(userCouponRepository, never()).save(any(UserCoupon.class));
        verify(rewardService, never()).spendPoints(any(User.class), any(Restaurant.class), anyInt(), anyString());
    }

    // ===================== 유저: 보유/사용 =====================

    @Test
    @DisplayName("보유 쿠폰 목록 조회")
    void getMyCoupons() {
        UserCoupon uc = new UserCoupon();
        uc.setUserCouponIdx(7);
        uc.setUser(customer);
        uc.setCoupon(coupon);
        uc.setUsed(false);
        uc.setIssuedAt(LocalDateTime.now());

        given(userCouponRepository.findByUserUserIdxOrderByIssuedAtDesc(2)).willReturn(List.of(uc));

        List<MyCouponResponseDto> result = couponService.getMyCoupons(2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserCouponIdx()).isEqualTo(7);
        assertThat(result.get(0).getTitle()).isEqualTo("아메리카노 무료");
        assertThat(result.get(0).getRestName()).isEqualTo("맛있는집");
        assertThat(result.get(0).getPointCost()).isEqualTo(300);
        assertThat(result.get(0).isUsed()).isFalse();
        assertThat(result.get(0).getUsedAt()).isNull();
    }

    @Test
    @DisplayName("쿠폰 사용 처리 성공")
    void useCoupon_success() {
        UserCoupon uc = new UserCoupon();
        uc.setUserCouponIdx(7);
        uc.setUser(customer);
        uc.setCoupon(coupon);
        uc.setUsed(false);

        given(userCouponRepository.findById(7)).willReturn(Optional.of(uc));

        couponService.useCoupon(7, 2);

        assertThat(uc.isUsed()).isTrue();
        assertThat(uc.getUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 보유 쿠폰 사용 시 IllegalStateException")
    void useCoupon_notFound() {
        given(userCouponRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.useCoupon(999, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("보유한 쿠폰을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("본인 쿠폰이 아니면 사용 시 SecurityException")
    void useCoupon_notOwner() {
        UserCoupon uc = new UserCoupon();
        uc.setUser(customer);
        uc.setCoupon(coupon);
        given(userCouponRepository.findById(7)).willReturn(Optional.of(uc));

        assertThatThrownBy(() -> couponService.useCoupon(7, 999))
                .isInstanceOf(SecurityException.class)
                .hasMessage("본인 쿠폰만 사용할 수 있습니다.");
    }

    @Test
    @DisplayName("이미 사용한 쿠폰은 다시 사용할 수 없다")
    void useCoupon_alreadyUsed() {
        UserCoupon uc = new UserCoupon();
        uc.setUser(customer);
        uc.setCoupon(coupon);
        uc.setUsed(true);
        given(userCouponRepository.findById(7)).willReturn(Optional.of(uc));

        assertThatThrownBy(() -> couponService.useCoupon(7, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 사용한 쿠폰입니다.");
    }
}
