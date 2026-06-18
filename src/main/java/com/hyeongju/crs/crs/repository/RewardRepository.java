package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardRepository extends JpaRepository<Reward, Integer> {

    // 유저의 누적 포인트 합계 (적립 내역이 없으면 0 반환)
    @Query("SELECT COALESCE(SUM(r.totalRewardValue), 0) FROM Reward r WHERE r.user.userIdx = :userIdx")
    int sumRewardValueByUserIdx(@Param("userIdx") int userIdx);
}
