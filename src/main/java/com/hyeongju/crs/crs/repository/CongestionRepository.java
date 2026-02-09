package com.hyeongju.crs.crs.repository;

import com.hyeongju.crs.crs.domain.Congestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


public interface CongestionRepository extends JpaRepository<Congestion, Integer> {

    List<Congestion> findByUserUserIdxOrderByCongAtDesc(int userIdx);


}
