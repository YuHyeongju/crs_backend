package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public Restaurant getOrCreateRestaurant(String kakaoId, String restName,String restAddress, String restTel){
        return restaurantRepository.findByKakaoId(kakaoId).orElseGet(()-> {
            Restaurant newRestaurant = new Restaurant();
            newRestaurant.setKakaoId(kakaoId);
            newRestaurant.setRestName(restName);
            newRestaurant.setRestAddress(restAddress);
            newRestaurant.setRestTel(restTel);
            return restaurantRepository.save(newRestaurant);
        });
    }

}
