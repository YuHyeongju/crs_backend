package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    // 특정 상인이 소유한 모든 가게의 쿠폰
    List<Coupon> findByRestaurant_User_UserIdx(int userIdx);

    // 유저에게 노출할 교환 가능한 쿠폰 (활성 + 유효기간 안 지남)
    @Query("SELECT c FROM Coupon c WHERE c.active = true AND (c.validUntil IS NULL OR c.validUntil >= :today)")
    List<Coupon> findAvailable(@Param("today") LocalDate today);

    // 특정 가게의 교환 가능한 쿠폰
    @Query("SELECT c FROM Coupon c WHERE c.restaurant.restIdx = :restIdx AND c.active = true AND (c.validUntil IS NULL OR c.validUntil >= :today)")
    List<Coupon> findAvailableByRestIdx(@Param("restIdx") int restIdx, @Param("today") LocalDate today);
}
