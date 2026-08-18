package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Congestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository; // import만 되어 있고 실제로는 사용되지 않음(아래 인터페이스에 @Repository가 붙어있지 않음)

import java.time.LocalDateTime;
import java.util.List;


public interface CongestionRepository extends JpaRepository<Congestion, Integer> {

    // 특정 유저(userIdx)의 혼잡도 제보 내역을 최신순으로 조회
    List<Congestion> findByUserUserIdxOrderByCongAtDesc(int userIdx);

    // 특정 유저가 제보한 총 횟수
    long countByUserUserIdx(int userIdx);

    // 같은 유저가 같은 가게를 특정 시각 이후에 제보한 적이 있는지 (리워드 30분 쿨다운 판단용)
    boolean existsByUserUserIdxAndRestaurantRestIdxAndCongAtAfter(int userIdx, int restIdx, LocalDateTime congAt);

}
