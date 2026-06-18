package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Integer> {

    List<UserCoupon> findByUserUserIdxOrderByIssuedAtDesc(int userIdx);
}
