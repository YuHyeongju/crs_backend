package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.RestaurantMenu;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    private final String uploadPath = "C:/upload/menu_picts/";

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

    @Transactional
    public Restaurant registerRestaurantByMerchant(RestaurantRequestDto dto, int userIdx,
                                                   List<MultipartFile>menuImages)throws IOException {
        User merchant = userRepository.findByUserIdx(userIdx)
                .orElseThrow(()-> new RuntimeException("상인 정보를 찾을 수 가 없습니다."));

        Restaurant restaurant = restaurantRepository.findByKakaoId(dto.getKakaoId())
                .orElseGet(() ->{
                    Restaurant newRestaurant = new Restaurant();
                    newRestaurant.setKakaoId(dto.getKakaoId());
                    return  newRestaurant;
                });

        restaurant.setRestName(dto.getRestName());
        restaurant.setRestAddress(dto.getRestAddress());
        restaurant.setRestTel(dto.getRestTel());
        restaurant.setRestBusiHours(dto.getRestBusiHours());

        restaurant.setStatus("ACTIVE");
        restaurant.setApprovalStatus("PENDING");
        restaurant.setUser(merchant);

        if(dto.getMenulist() != null){

            int index = 0;
            for(RestaurantRequestDto.MenuList menuDto : dto.getMenulist()){
                RestaurantMenu menu = new RestaurantMenu();
                menu.setMenuName(menuDto.getMenuName());
                menu.setMenuPrice(menuDto.getMenuPrice());

                if(menuImages != null && index < menuImages.size()){
                    MultipartFile imageFile = menuImages.get(index);
                    if(!imageFile.isEmpty()){
                        String savedName = saveImage(imageFile);
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
    private String saveImage(MultipartFile file) throws IOException {
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if(!created){
                throw new IOException("폴더 생성 실패" + uploadPath);
            }
        }

        String originFilename = file.getOriginalFilename();
        String uuid = UUID.randomUUID().toString();
        String saveName = uuid + "_" + originFilename;

        File target = new File(uploadPath + saveName);
        file.transferTo(target);

        return saveName;
    }
}



