package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.RestaurantPinDto;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
import com.hyeongju.crs.crs.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/detail")
    public ResponseEntity<Restaurant> restaurantDetail(@RequestBody RestaurantRequestDto dto) {
        Restaurant restaurant = restaurantService.getOrCreateRestaurant(
                dto.getKakaoId(), dto.getRestName(), dto.getRestAddress(), dto.getRestTel());
        return ResponseEntity.ok(restaurant);
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerRestaurant(@RequestPart("dto") RestaurantRequestDto dto,
                                                @RequestPart(value = "menuImages", required = false)
                                                List<MultipartFile> menuImages,
                                                HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            Restaurant result = restaurantService.registerRestaurantByMerchant(dto, userIdx, menuImages);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("등록 중에 오류 발생:" + e.getMessage());
        }
    }

    @GetMapping("/my-restaurant-list")
    public ResponseEntity<?> getMyRestaurants(HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            List<RestaurantResponseDto> myRestaurants = restaurantService.getMyRestaurants(userIdx);
            return ResponseEntity.ok(myRestaurants);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("가게 목록을 조회할 수 없음" + e.getMessage());
        }
    }

    @GetMapping("/merchant-pins")
    public ResponseEntity<List<RestaurantPinDto>> getMerchantPins() {
        return ResponseEntity.ok(restaurantService.getApprovedMerchantPins());
    }

    @GetMapping("/restIdx/{restIdx}")
    public ResponseEntity<RestaurantPinDto> getRestaurantByRestIdx(@PathVariable("restIdx") int restIdx) {
        return ResponseEntity.ok(restaurantService.getRestaurantPinByRestIdx(restIdx));
    }

    @PostMapping("/bulkDetails")
    public ResponseEntity<Map<String, RestaurantResponseDto>> getBulkDetailsByKakaoIds(@RequestBody List<String> kakaoIds) {
        try {
            return ResponseEntity.ok(restaurantService.getBulkDetailsByKakaoIds(kakaoIds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/kakaoId/{kakaoId}")
    public ResponseEntity<RestaurantResponseDto> getRestaurantDetailByKakaoId(@PathVariable("kakaoId") String kakaoId) {
        try {
            return ResponseEntity.ok(restaurantService.getRestaurantDetailsByKakaoId(kakaoId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/edit/{restIdx}")
    public ResponseEntity<?> getRestaurantForEdit(@PathVariable("restIdx") int restIdx, HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            return ResponseEntity.ok(restaurantService.getRestaurantForEdit(restIdx));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping(value = "/update/{restIdx}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateRestaurant(@PathVariable("restIdx") Integer restIdx,
                                              @RequestPart("dto") RestaurantRequestDto dto,
                                              @RequestPart(value = "menuImages", required = false)
                                              List<MultipartFile> menuImages,
                                              HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            restaurantService.updateRestaurantByMerchant(restIdx, dto, menuImages);
            return ResponseEntity.ok("가게 정보가 업데이트 되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("수정실패" + e.getMessage());
        }
    }

    @PostMapping("/delete/{restIdx}")
    public ResponseEntity<String> deleteRestaurant(@PathVariable("restIdx") int restIdx) {
        try {
            restaurantService.deleteRestaurant(restIdx);
            return ResponseEntity.ok("식당 정보와 메뉴 사진이 모두 삭제 됨");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("삭제 중에 오류가 발생했습니다." + e.getMessage());
        }
    }
}
