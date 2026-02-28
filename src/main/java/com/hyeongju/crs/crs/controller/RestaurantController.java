package com.hyeongju.crs.crs.controller;


import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.service.RestaurantService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/detail")
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

    @PostMapping(value = "/register",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerRestaurant(@RequestPart("dto") RestaurantRequestDto dto,
                                                @RequestPart(value = "menuImages", required = false)
                                                List<MultipartFile> menuImages, HttpSession session){



        Integer userIdx = (Integer) session.getAttribute("userIdx");

        if(userIdx == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            Restaurant result = restaurantService.registerRestaurantByMerchant(dto, userIdx, menuImages);
            System.out.println("식당 및 메뉴 사진 등록 성공");

            return ResponseEntity.ok(result);

        }catch (Exception e){
            System.out.println(">>> [디버깅] 등록 실패한 DTO: " + dto.toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("등록 중에 오류 발생:" +
                    e.getMessage());



        }

    }
}
