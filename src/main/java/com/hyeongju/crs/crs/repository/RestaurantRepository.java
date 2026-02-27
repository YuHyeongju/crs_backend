package com.hyeongju.crs.crs.repository;


import com.hyeongju.crs.crs.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {


    Optional <Restaurant> findByKakaoId(String kakaoId);

    Optional <Restaurant> findByRestIdx(int restIdx);

    List <Restaurant> findByApprovalStatus(String approvalStatus);
}
