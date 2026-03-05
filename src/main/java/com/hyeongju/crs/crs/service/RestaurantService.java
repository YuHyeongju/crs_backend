package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.RestaurantFacilities;
import com.hyeongju.crs.crs.domain.RestaurantMenu;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    private final String uploadPath = "C:/upload/menu_picts/";

    @Transactional
    public Restaurant getOrCreateRestaurant(String kakaoId, String restName, String restAddress, String restTel) {
        return restaurantRepository.findByKakaoId(kakaoId).orElseGet(() -> {
            Restaurant newRestaurant = new Restaurant();
            newRestaurant.setKakaoId(kakaoId);
            newRestaurant.setRestName(restName);
            newRestaurant.setRestAddress(restAddress);
            newRestaurant.setRestTel(restTel);
            return restaurantRepository.save(newRestaurant);
        });
    }

    @Transactional
    public Restaurant registerRestaurantByMerchant(RestaurantRequestDto dto, int userIdx,
                                                   List<MultipartFile> menuImages) throws IOException {

        User merchant = userRepository.findByUserIdx(userIdx)
                .orElseThrow(() -> new RuntimeException("상인 정보를 찾을 수 가 없습니다."));

        String merchantName = merchant.getName();


        Restaurant restaurant = restaurantRepository.findByKakaoId(dto.getKakaoId())
                .orElseGet(() -> {
                    Restaurant newRestaurant = new Restaurant();
                    newRestaurant.setKakaoId(dto.getKakaoId());
                    return newRestaurant;
                });

        restaurant.setRestName(dto.getRestName());
        restaurant.setRestAddress(dto.getRestAddress());
        restaurant.setRestTel(dto.getRestTel());
        restaurant.setRestBusiHours(dto.getRestBusiHours());

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

                System.out.println(">>> [백엔드 디버깅] 메뉴 이름: " + menuDto.getMenuName());
                System.out.println(">>> [백엔드 디버깅] 메뉴 가격: " + menuDto.getMenuPrice());

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
            return dto;
        }).collect(Collectors.toList());
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








