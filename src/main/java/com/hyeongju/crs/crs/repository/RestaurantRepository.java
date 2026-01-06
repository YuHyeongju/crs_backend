package com.hyeongju.crs.crs.repository;


import com.hyeongju.crs.crs.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {


    Optional <Restaurant> findByKakaoId(String kakaoId);

    // save, findById, existsById, deleteById 등은 상속받아서 이미 구현되 있음.
}
