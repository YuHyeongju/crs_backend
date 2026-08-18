package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Integer> {

    // 특정 유저(userIdx)가 보유한 쿠폰 목록을 발급받은 시각 최신순으로 조회
    List<UserCoupon> findByUserUserIdxOrderByIssuedAtDesc(int userIdx);
}
