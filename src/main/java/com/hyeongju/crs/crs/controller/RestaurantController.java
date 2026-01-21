package com.hyeongju.crs.crs.controller;


import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;


    public ResponseEntity<Restaurant> restaurantDetail(@RequestBody RestaurantRequestDto dto){
        // 서비스에있는 getOrCreateRestaurant를 호출해서 식당 불러옴\
        Restaurant restaurant = restaurantService.getOrCreateRestaurant(
                dto.getKakaoId(),
                dto.getRestName(),
                dto.getRestAddress(),
                dto.getRestTel()
        );
        return ResponseEntity.ok(restaurant);
    }

}
