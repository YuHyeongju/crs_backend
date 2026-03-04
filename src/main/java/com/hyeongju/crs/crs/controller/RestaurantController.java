package com.hyeongju.crs.crs.controller;


import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
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
        System.out.println(">>> [백엔드 디버깅] 컨트롤러 진입 - DTO 데이터: " + dto.toString());
        if (dto.getFacilities() != null) {
            System.out.println(">>> [백엔드 디버깅] facilities 데이터 존재: " + dto.getFacilities());
        } else {
            System.out.println(">>> [백엔드 디버깅] facilities 데이터가 null입니다.");
        }


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
    @GetMapping("/my-restaurant-list")
    public ResponseEntity<?> getMyRestaurants(HttpSession session){
        Integer userIdx = (Integer) session.getAttribute("userIdx");

        if(userIdx == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");

        }

        try {
            List<RestaurantResponseDto> myRestaurants = restaurantService.getMyRestaurants(userIdx);

            return ResponseEntity.ok(myRestaurants);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("가게 목록을 조회할 수 없음" + e.getMessage());
        }
    }

    @GetMapping("/{restIdx}")
    public ResponseEntity<?> getRestaurantDetail(@PathVariable("restIdx") Integer restIdx, HttpSession session) {
        // 보안을 위한 세션 체크
        Integer userIdx = (Integer) session.getAttribute("userIdx");
        if (userIdx == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {

            RestaurantRequestDto dto = restaurantService.getRestaurantForEdit(restIdx);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("가게 정보를 불러오지 못했습니다: " + e.getMessage());
        }
    }



    @PostMapping(value = "/update/{restIdx}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateRestaurant(@PathVariable("restIdx")Integer restIdx,
                                              @RequestPart("dto") RestaurantRequestDto dto,
                                              @RequestPart(value = "menuImages", required = false)
                                                  List<MultipartFile> menuImages,
                                              HttpSession session){
        Integer userIdx = (Integer)session. getAttribute("userIdx");
        if(userIdx == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("로그인이 필요합니다.");

        try{
            Restaurant result = restaurantService.updateRestaurantByMerchant(restIdx,dto,menuImages);
            return ResponseEntity.ok("가게 정보가 업데이트 되었습니다.");
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("수정실패" + e.getMessage());
        }
    }

    @PostMapping("/delete/{restIdx}")
    public ResponseEntity<String> deleteRestaurant(@PathVariable("restIdx") int restIdx){
        try {
            System.out.println("삭제할 식당번호" + restIdx);

            restaurantService.deleteRestaurant(restIdx);

            return  ResponseEntity.ok("식당 정보와 메뉴 사진이 모두 삭제 됨");
        }catch(IllegalStateException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("삭제 중에 오류가 발생했습니다." + e.getMessage());
        }

    }
}
