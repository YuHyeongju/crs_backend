package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.MenuResponseDto;
import com.hyeongju.crs.crs.dto.RestaurantPinDto;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
import com.hyeongju.crs.crs.service.RestaurantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// 가게(음식점) API: 등록/조회/수정/삭제 + 지도 핀/메뉴 조회
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    // 카카오맵 정보 기준으로 가게를 조회하고, 없으면 새로 생성(즐겨찾기 등에서 사용)
    @PostMapping("/detail")
    public ResponseEntity<Restaurant> restaurantDetail(@Valid @RequestBody RestaurantRequestDto dto) {
        Restaurant restaurant = restaurantService.getOrCreateRestaurant(
                dto.getKakaoId(), dto.getRestName(), dto.getRestAddress(), dto.getRestTel());
        return ResponseEntity.ok(restaurant);
    }

    // 상인의 가게 등록(메뉴 이미지 포함 멀티파트 업로드, 승인 대기 상태로 생성)
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerRestaurant(@Valid @RequestPart("dto") RestaurantRequestDto dto,
                                                @RequestPart(value = "menuImages", required = false)
                                                List<MultipartFile> menuImages,
                                                HttpServletRequest request) throws IOException {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            Restaurant result = restaurantService.registerRestaurantByMerchant(dto, userIdx, menuImages);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // 로그인한 상인 본인이 등록한 가게 목록 조회
    @GetMapping("/my-restaurant-list")
    public ResponseEntity<?> getMyRestaurants(HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        List<RestaurantResponseDto> myRestaurants = restaurantService.getMyRestaurants(userIdx);
        return ResponseEntity.ok(myRestaurants);
    }

    // 승인된 상인 가게들의 지도 핀 목록(비로그인 사용자도 조회 가능)
    @GetMapping("/merchant-pins")
    public ResponseEntity<List<RestaurantPinDto>> getMerchantPins() {
        return ResponseEntity.ok(restaurantService.getApprovedMerchantPins());
    }

    // 가게 idx로 지도 핀 정보 단건 조회
    @GetMapping("/restIdx/{restIdx}")
    public ResponseEntity<RestaurantPinDto> getRestaurantByRestIdx(@PathVariable("restIdx") int restIdx) {
        return ResponseEntity.ok(restaurantService.getRestaurantPinByRestIdx(restIdx));
    }

    // 가게 idx로 메뉴 목록 조회
    @GetMapping("/{restIdx}/menus")
    public ResponseEntity<List<MenuResponseDto>> getMenusByRestIdx(@PathVariable("restIdx") int restIdx) {
        return ResponseEntity.ok(restaurantService.getMenusByRestIdx(restIdx));
    }

    // 지도에 표시된 여러 가게의 상세 정보를 카카오ID 목록으로 한 번에 조회
    @PostMapping("/bulkDetails")
    public ResponseEntity<Map<String, RestaurantResponseDto>> getBulkDetailsByKakaoIds(@RequestBody List<String> kakaoIds) {
        try {
            return ResponseEntity.ok(restaurantService.getBulkDetailsByKakaoIds(kakaoIds));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 카카오맵 가게 ID로 상세 정보 조회
    @GetMapping("/kakaoId/{kakaoId}")
    public ResponseEntity<RestaurantResponseDto> getRestaurantDetailByKakaoId(@PathVariable("kakaoId") String kakaoId) {
        try {
            return ResponseEntity.ok(restaurantService.getRestaurantDetailsByKakaoId(kakaoId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 상인이 본인 가게 정보를 수정 화면에서 불러올 때 사용(수정 폼 초기값)
    @GetMapping("/edit/{restIdx}")
    public ResponseEntity<?> getRestaurantForEdit(@PathVariable("restIdx") int restIdx, HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            return ResponseEntity.ok(restaurantService.getRestaurantForEdit(restIdx, userIdx));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 상인의 가게 정보 수정(메뉴 이미지 추가/교체 포함)
    @PostMapping(value = "/update/{restIdx}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateRestaurant(@PathVariable("restIdx") Integer restIdx,
                                              @Valid @RequestPart("dto") RestaurantRequestDto dto,
                                              @RequestPart(value = "menuImages", required = false)
                                              List<MultipartFile> menuImages,
                                              HttpServletRequest request) throws IOException {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            restaurantService.updateRestaurantByMerchant(restIdx, userIdx, dto, menuImages);
            return ResponseEntity.ok("가게 정보가 업데이트 되었습니다.");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 가게 삭제(연관된 메뉴 사진 파일까지 함께 정리) - 본인이 등록한 가게인지 확인 후 삭제
    @PostMapping("/delete/{restIdx}")
    public ResponseEntity<String> deleteRestaurant(@PathVariable("restIdx") int restIdx, HttpServletRequest request) {
        Integer userIdx = (Integer) request.getAttribute("authenticatedUserIdx");
        if (userIdx == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        try {
            restaurantService.deleteRestaurant(restIdx, userIdx);
            return ResponseEntity.ok("식당 정보와 메뉴 사진이 모두 삭제 됨");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
