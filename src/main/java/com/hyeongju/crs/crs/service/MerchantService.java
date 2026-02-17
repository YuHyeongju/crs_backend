package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class MerchantService extends AbstractRegistrationService {

    public MerchantService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ){
        super(userRepository,roleRepository,passwordEncoder);
    }

    @Transactional
    public User registerMerchant(MerchantRegistractionDto dto){

        if(!isVaildBussinessNumber(dto.getBusinessNum())){
            throw new IllegalArgumentException("유효하지 않은 사업자 등록번호 입니다.");
        }

        User newUser = super.registerCommonFields(dto, RoleName.MERCHANT);

        newUser.setBusinessNum(dto.getBusinessNum());

        return userRepository.save(newUser);


    }
    // 사업자 등록번호 유효성 검사
    private boolean isVaildBussinessNumber(String businessNum){
        return businessNum != null && businessNum.length() == 10;
    }

    public MypageResponseDto getMerchantProfile(int userIdx){
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        MypageResponseDto dto = new MypageResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setRole(user.getRole().getRoleName().name());
        dto.setPhNum(user.getPhNum());
        dto.setBusinessNum(user.getBusinessNum());
        return dto;
    }

    @Transactional
    public void updateMerchantProfile(int userIdx, MerchantUpdateDto dto){
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()) {
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
        user.setBusinessNum(dto.getBusinessNum());
    }
}
