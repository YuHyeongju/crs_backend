package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.RestaurantFacilities;
import com.hyeongju.crs.crs.domain.RestaurantMenu;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MenuResponseDto;
import com.hyeongju.crs.crs.dto.RestaurantPinDto;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    private final String uploadPath = "C:/upload/menu_picts/";



    public Restaurant getRestaurantByRestIdx(int restIdx) {
        return restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalArgumentException("가게를 찾을 수 없습니다: " + restIdx));
    }

    @Transactional
    public Restaurant getOrCreateRestaurant(String kakaoId, String restName, String restAddress, String restTel) {

        return restaurantRepository.findByKakaoId(kakaoId)
                .map(existing -> {
                    boolean changed = false;
                    if (isBlank(existing.getRestName()) && !isBlank(restName)) {
                        existing.setRestName(restName);
                        changed = true;
                    }
                    if (isBlank(existing.getRestAddress()) && !isBlank(restAddress)) {
                        existing.setRestAddress(restAddress);
                        changed = true;
                    }
                    if (isBlank(existing.getRestTel()) && !isBlank(restTel)) {
                        existing.setRestTel(restTel);
                        changed = true;
                    }
                    return changed ? restaurantRepository.saveAndFlush(existing) : existing;
                })
                .orElseGet(() -> {
                    try {
                        Restaurant newRestaurant = new Restaurant();
                        newRestaurant.setKakaoId(kakaoId);
                        newRestaurant.setRestName(restName);
                        newRestaurant.setRestAddress(restAddress);
                        newRestaurant.setRestTel(restTel);
                        return restaurantRepository.saveAndFlush(newRestaurant);
                    } catch (DataIntegrityViolationException e) {
                        return restaurantRepository.findByKakaoId(kakaoId)
                                .orElseThrow(() -> new RuntimeException("가게 정보 등록 중 동시성 오류 발생"));
                    }
                });
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Transactional
    public Restaurant registerRestaurantByMerchant(RestaurantRequestDto dto, int userIdx,
                                                   List<MultipartFile> menuImages) throws IOException {

        User merchant = userRepository.findByUserIdx(userIdx)
                .orElseThrow(() -> new RuntimeException("상인 정보를 찾을 수 가 없습니다."));

        String merchantName = merchant.getName();


        Restaurant restaurant = isBlank(dto.getKakaoId())
                ? new Restaurant()
                : restaurantRepository.findByKakaoId(dto.getKakaoId())
                    .orElseGet(() -> {
                        Restaurant newRestaurant = new Restaurant();
                        newRestaurant.setKakaoId(dto.getKakaoId());
                        return newRestaurant;
                    });

        // 이미 다른 상인이 등록(claim)한 가게면 거절 — 본인이 이미 가진 가게는 재등록(수정) 허용
        if (restaurant.getUser() != null && restaurant.getUser().getUserIdx() != userIdx) {
            throw new IllegalStateException("이미 다른 사업자가 등록한 가게입니다.");
        }

        restaurant.setRestName(dto.getRestName());
        restaurant.setRestAddress(dto.getRestAddress());
        restaurant.setRestTel(dto.getRestTel());
        restaurant.setRestBusiHours(dto.getRestBusiHours());

        if (dto.getLatitude() != null) restaurant.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) restaurant.setLongitude(dto.getLongitude());

        restaurant.setStatus("ACTIVE");
        restaurant.setApprovalStatus("PENDING");
        restaurant.setUser(merchant);

        if (dto.getFacilities() != null) {
            RestaurantFacilities facilities = new RestaurantFacilities();
            RestaurantRequestDto.FacilitiesDto fDto = dto.getFacilities();

            facilities.setWifi(fDto.isWifi());
            facilities.setRestRoom(fDto.isRestRoom());
            facilities.setParkingAvailable(fDto.isParkingAvailable());
            facilities.setPackingPossible(fDto.isPackingPossible());
            facilities.setKakaoPay(fDto.isKakaoPay());
            facilities.setSamsungPay(fDto.isSamsungPay());
            facilities.setKiosk(fDto.isKiosk());

            facilities.setRestaurant(restaurant);
            restaurant.getFacilities().add(facilities);
        }

        if (dto.getMenulist() != null) {

            int index = 0;
            for (RestaurantRequestDto.MenuList menuDto : dto.getMenulist()) {
                RestaurantMenu menu = new RestaurantMenu();

                System.out.println(">>> 메뉴 이름: " + menuDto.getMenuName());
                System.out.println(">>> 메뉴 가격: " + menuDto.getMenuPrice());

                menu.setMenuName(menuDto.getMenuName());
                menu.setMenuPrice(menuDto.getMenuPrice());

                if (menuImages != null && index < menuImages.size()) {
                    MultipartFile imageFile = menuImages.get(index);
                    if (!imageFile.isEmpty()) {
                        String savedName = saveImage(imageFile,merchantName);
                        menu.setMenuPict(savedName);
                    }
                }

                menu.setRestaurant(restaurant);

                restaurant.getMenuList().add(menu);

                index++;
            }
        }

        return restaurantRepository.save(restaurant);
    }

    private String saveImage(MultipartFile file, String userName) throws IOException {
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IOException("폴더 생성 실패" + uploadPath);
            }
        }

        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());

        String saveName = timeStamp + "_" + userName;

        File target = new File(uploadPath + saveName);
        file.transferTo(target);

        return saveName;
    }

    public List<RestaurantResponseDto> getMyRestaurants(int userIdx) {
        List<Restaurant> restaurants = restaurantRepository.findByUserUserIdxAndApprovalStatus(
                userIdx,"APPROVED");

        return restaurants.stream().map(restaurant -> {
            RestaurantResponseDto dto = new RestaurantResponseDto();
            dto.setRestIdx(restaurant.getRestIdx());
            dto.setRestName(restaurant.getRestName());
            dto.setRestAddress(restaurant.getRestAddress());

            // Calculate average rating and review count
            Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                                                .orElse(0.0); // Default to 0.0 if no reviews
            Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

            dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0); // Round to one decimal place
            dto.setReviewCount(reviewCount);

            return dto;
        }).collect(Collectors.toList());
    }

    public RestaurantResponseDto getRestaurantDetails(int restIdx) {

        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("해당 식당 정보를 찾을 수 없음: " + restIdx));

        RestaurantResponseDto dto = new RestaurantResponseDto();
        dto.setRestIdx(restaurant.getRestIdx());
        dto.setRestName(restaurant.getRestName());
        dto.setRestAddress(restaurant.getRestAddress());

        Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                                            .orElse(0.0);
        Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

        dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0);
        dto.setReviewCount(reviewCount);

        return dto;
    }

    // 여러 카카오ID의 평점/리뷰수를 한 번에 — 지도 핀 로딩 최적화용
    public Map<String, RestaurantResponseDto> getBulkDetailsByKakaoIds(List<String> kakaoIds) {
        Map<String, RestaurantResponseDto> result = new HashMap<>();
        if (kakaoIds == null || kakaoIds.isEmpty()) return result;

        List<Restaurant> restaurants = restaurantRepository.findByKakaoIdIn(kakaoIds);
        if (restaurants.isEmpty()) return result;

        List<Integer> restIdxes = restaurants.stream()
                .map(Restaurant::getRestIdx)
                .collect(Collectors.toList());

        // restIdx -> [avg, count]
        Map<Integer, double[]> statsMap = new HashMap<>();
        for (Object[] row : reviewRepository.findRatingStatsByRestIdxIn(restIdxes)) {
            Integer restIdx = (Integer) row[0];
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long count = ((Number) row[2]).longValue();
            statsMap.put(restIdx, new double[]{avg, count});
        }

        for (Restaurant r : restaurants) {
            double[] stats = statsMap.getOrDefault(r.getRestIdx(), new double[]{0.0, 0});
            RestaurantResponseDto dto = new RestaurantResponseDto();
            dto.setRestIdx(r.getRestIdx());
            dto.setRestName(r.getRestName());
            dto.setRestAddress(r.getRestAddress());
            dto.setAverageRating(Math.round(stats[0] * 10.0) / 10.0);
            dto.setReviewCount((int) stats[1]);
            result.put(r.getKakaoId(), dto);
        }
        return result;
    }

    @Transactional
    public RestaurantResponseDto getRestaurantDetailsByKakaoId(String kakaoId) {
        return restaurantRepository.findByKakaoId(kakaoId)
                .map(restaurant -> {
                    RestaurantResponseDto dto = new RestaurantResponseDto();
                    dto.setRestIdx(restaurant.getRestIdx());
                    dto.setRestName(restaurant.getRestName());
                    dto.setRestAddress(restaurant.getRestAddress());

                    Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                                                        .orElse(0.0);
                    Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

                    dto.setAverageRating(Math.round(averageRating * 10.0) / 10.0);
                    dto.setReviewCount(reviewCount);
                    // 가게를 등록한 상인 식별자 — 미등록(카카오 자동생성) 식당이면 null
                    dto.setOwnerUserIdx(restaurant.getUser() != null ? restaurant.getUser().getUserIdx() : null);
                    return dto;
                })
                // DB 미등록 카카오ID: 빈 DTO 반환 (지도에서 처음 보는 식당 대응)
                .orElseGet(() -> new RestaurantResponseDto(null, null, null, 0.0, 0, null));
    }



    @Transactional
    public RestaurantRequestDto getRestaurantForEdit(int restIdx) {

        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("해당 식당 정보를 찾을 수 없음: " + restIdx));


        RestaurantRequestDto dto = new RestaurantRequestDto();
        dto.setRestName(restaurant.getRestName());
        dto.setRestAddress(restaurant.getRestAddress());
        dto.setRestTel(restaurant.getRestTel());
        dto.setRestBusiHours(restaurant.getRestBusiHours());


        if (!restaurant.getFacilities().isEmpty()) {
            RestaurantFacilities facilities = restaurant.getFacilities().get(0);
            RestaurantRequestDto.FacilitiesDto fDto = new RestaurantRequestDto.FacilitiesDto();
            fDto.setWifi(facilities.isWifi());
            fDto.setRestRoom(facilities.isRestRoom());
            fDto.setParkingAvailable(facilities.isParkingAvailable());
            fDto.setPackingPossible(facilities.isPackingPossible());
            fDto.setKakaoPay(facilities.isKakaoPay());
            fDto.setSamsungPay(facilities.isSamsungPay());
            fDto.setKiosk(facilities.isKiosk());
            dto.setFacilities(fDto);
        }


        List<RestaurantRequestDto.MenuList> menuListDtos = new ArrayList<>();
        for (RestaurantMenu menu : restaurant.getMenuList()) {
            RestaurantRequestDto.MenuList mDto = new RestaurantRequestDto.MenuList();
            mDto.setMenuName(menu.getMenuName());
            mDto.setMenuPrice(menu.getMenuPrice());
            mDto.setMenuPict(menu.getMenuPict()); // 기존 저장된 이미지 파일명
            menuListDtos.add(mDto);
        }
        dto.setMenulist(menuListDtos);

        return dto;
    }

    @Transactional
    public Restaurant updateRestaurantByMerchant(int restIdx, RestaurantRequestDto dto,
                                                 List<MultipartFile> menuImages) throws IOException {
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("수정할 식당 정보를 찾을 수 없음"));
        String merchantName = restaurant.getUser().getName();

        restaurant.setRestName(dto.getRestName());
        restaurant.setRestAddress(dto.getRestAddress());
        restaurant.setRestTel(dto.getRestTel());
        restaurant.setRestBusiHours(dto.getRestBusiHours());

        if (dto.getFacilities() != null) {
            RestaurantFacilities facilities = restaurant.getFacilities().isEmpty() ? new RestaurantFacilities() :
                    restaurant.getFacilities().get(0);

            RestaurantRequestDto.FacilitiesDto fDto = dto.getFacilities();
            facilities.setWifi(fDto.isWifi());
            facilities.setRestRoom(fDto.isRestRoom());
            facilities.setParkingAvailable(fDto.isParkingAvailable());
            facilities.setPackingPossible(fDto.isPackingPossible());
            facilities.setKakaoPay(fDto.isKakaoPay());
            facilities.setSamsungPay(fDto.isSamsungPay());
            facilities.setKiosk(fDto.isKiosk());

            if (restaurant.getFacilities().isEmpty()) {
                facilities.setRestaurant(restaurant);
                restaurant.getFacilities().add(facilities);
            }
        }
        if (dto.getMenulist() != null) {
            restaurant.getMenuList().clear();

            int index = 0;
            for (RestaurantRequestDto.MenuList menuDto : dto.getMenulist()) {
                RestaurantMenu menu = new RestaurantMenu();
                menu.setMenuName(menuDto.getMenuName());
                menu.setMenuPrice(menuDto.getMenuPrice());

                if (menuImages != null && index < menuImages.size()) {
                    MultipartFile imageFile = menuImages.get(index);
                    if (!imageFile.isEmpty()) {
                        String saveName = saveImage(imageFile, merchantName);
                        menu.setMenuPict(saveName);
                    }
                }
                menu.setRestaurant(restaurant);
                restaurant.getMenuList().add(menu);
                index++;
            }
        }
        return restaurant;
    }


    public List<MenuResponseDto> getMenusByRestIdx(int restIdx) {
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("해당 식당 정보를 찾을 수 없음: " + restIdx));
        return restaurant.getMenuList().stream()
                .map(menu -> new MenuResponseDto(
                        menu.getMenuIdx(),
                        menu.getMenuName(),
                        menu.getMenuPrice(),
                        menu.getMenuPict() != null ? "http://localhost:8080/uploads/" + menu.getMenuPict() : null
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteRestaurant(int restIdx){
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new IllegalStateException("삭제할 식당을 찾을 수 없습니다."));

        if(restaurant.getMenuList() != null){
            for(RestaurantMenu menu : restaurant.getMenuList()){
                if(menu.getMenuPict() != null && !menu.getMenuPict().isEmpty()){
                    deleteActualFile(menu.getMenuPict());
                }
            }
        }
        restaurantRepository.delete(restaurant);
    }

    public List<RestaurantPinDto> getApprovedMerchantPins() {
        return restaurantRepository
                .findByUserIsNotNullAndApprovalStatusAndLatitudeIsNotNullAndLongitudeIsNotNull("APPROVED")
                .stream()
                .map(this::toRestaurantPinDto)
                .collect(Collectors.toList());
    }

    public RestaurantPinDto getRestaurantPinByRestIdx(int restIdx) {
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(() -> new IllegalStateException("Restaurant not found: " + restIdx));

        return toRestaurantPinDto(restaurant);
    }

    private RestaurantPinDto toRestaurantPinDto(Restaurant restaurant) {
        Double averageRating = reviewRepository.findAverageRatingByRestaurantRestIdx(restaurant.getRestIdx())
                .orElse(0.0);
        Integer reviewCount = reviewRepository.countByRestaurantRestIdx(restaurant.getRestIdx());

        return new RestaurantPinDto(
                restaurant.getRestIdx(),
                restaurant.getRestName(),
                restaurant.getRestAddress(),
                restaurant.getRestTel(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getKakaoId(),
                Math.round(averageRating * 10.0) / 10.0,
                reviewCount,
                restaurant.getUser() != null ? restaurant.getUser().getUserIdx() : null
        );
    }

    private void deleteActualFile(String fileName){
        File file = new File(uploadPath + fileName);
        if(file.exists()){
            if(file.delete()){
                System.out.println("파일 삭제 완료" + fileName);
            }else{
                System.out.println("파일 삭제 실패" + fileName);
            }
        }
    }
}








