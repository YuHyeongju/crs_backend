package com.hyeongju.crs.crs.service;


import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.AdminUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AdminService extends AbstractRegistrationService {

    private final RestaurantRepository restaurantRepository;


    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            RestaurantRepository restaurantRepository
    ){
        super(userRepository,roleRepository,passwordEncoder);

        this.restaurantRepository = restaurantRepository;
    }

    @Transactional
    public User registerAdmin(AdminRegistractionDto dto){
        if(!isVaildAdminNum(dto.getAdminNum())){
            throw new IllegalArgumentException("유효하지 않은 관리자 코드 입니다.");
        }

        User newUser = super.registerCommonFields(dto, RoleName.ADMIN);

        newUser.setAdminNum(dto.getAdminNum());

        return userRepository.save(newUser);
    }

    private boolean isVaildAdminNum(String adminNum){
        return adminNum != null && adminNum.length() == 7;
    }

    public MypageResponseDto getAdminProfile(int userIdx){
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        MypageResponseDto dto = new MypageResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setRole(user.getRole().getRoleName().name());
        dto.setPhNum(user.getPhNum());
        dto.setAdminNum(user.getAdminNum());
        return dto;
    }

    @Transactional
    public void updateAdminProfile(int userIdx, AdminUpdateDto dto){
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()) {
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
        user.setAdminNum(dto.getAdminNum());
    }

    public List<Restaurant> getPendingRestaurant(){

        return restaurantRepository.findByApprovalStatus("PENDING");

    }
    @Transactional
    public void approvalRestaurant(int restIdx){
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new RuntimeException("가게를 찾을 수 없음"));

        restaurant.setApprovalStatus("APPROVED");
        restaurant.setStatus("ACTIVE");

        restaurantRepository.save(restaurant);
    }
    
    @Transactional
    public void rejectRestaurant(int restIdx){
        Restaurant restaurant = restaurantRepository.findByRestIdx(restIdx)
                .orElseThrow(()-> new RuntimeException("가게를 찾을 수 없음"));
        restaurant.setApprovalStatus("REJECT");
        restaurant.setStatus("ACTIVE");

        restaurantRepository.save(restaurant);
    }
    



}
