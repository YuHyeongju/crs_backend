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
    // 상인(가게 사장님) 회원가입/조회/수정 로직

    public MerchantService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ){
        super(userRepository,roleRepository,passwordEncoder);
    }

    @Transactional
    public User registerMerchant(MerchantRegistractionDto dto){
        // 상인 회원가입: 사업자번호 검증 → 공통 필드 생성 → 사업자번호 세팅 → 저장

        if(!isVaildBussinessNumber(dto.getBusinessNum())){
            throw new IllegalArgumentException("유효하지 않은 사업자 등록번호 입니다.");
        }

        User newUser = super.registerCommonFields(dto, RoleName.MERCHANT);

        newUser.setBusinessNum(dto.getBusinessNum());

        return userRepository.save(newUser);


    }
    private boolean isVaildBussinessNumber(String businessNum){
        return businessNum != null && businessNum.length() == 12; // 하이픈 포함 12자리("123-45-67890")인지만 간단히 체크 — 형식 자체는 DTO의 @Pattern에서 이미 검증됨
    }

    public MypageResponseDto getMerchantProfile(int userIdx){
        // 마이페이지 조회: User 엔티티를 MypageResponseDto로 매핑
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
        // 마이페이지 수정: 비밀번호는 입력됐을 때만 재암호화 후 갱신
        User user = userRepository.findByUserIdx(userIdx).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()) {
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
        user.setBusinessNum(dto.getBusinessNum());
        // save() 호출 없이도 @Transactional 안에서 dirty checking으로 자동 UPDATE됨
    }
}
