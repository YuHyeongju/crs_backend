package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Congestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


public interface CongestionRepository extends JpaRepository<Congestion, Integer> {

    List<Congestion> findByUserUserIdxOrderByCongAtDesc(int userIdx);

    long countByUserUserIdx(int userIdx);

    // 같은 유저가 같은 가게를 특정 시각 이후에 제보한 적이 있는지 (리워드 30분 쿨다운 판단용)
    boolean existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(int userIdx, int restIdx, LocalDateTime congAt);

}
