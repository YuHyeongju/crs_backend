package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.*;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService extends AbstractRegistrationService {
    // 부모 클래스의 생성자를 호출해서 필드에 주입함.
    public UserService(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passWordEncoders
    ){
        super(userRepository,roleRepository,passWordEncoders);
    }

    @Transactional // 트랜잭션에 오류가 발생하면 작업이 취소되고 롤백되고 변경사항이 취소됨
    public User registerUser(UserRegistractionDto dto){
        // 부모한테서 registerCommonFields 호출
        User newUser = super.registerCommonFields(dto, RoleName.USER);

        return userRepository.save(newUser);
    }
    //todo: id로 사용자를 찾는 것을 userIdx로 찾는 것으로 변경 요망                            
    public MypageResponseDto getMyProfile(String id){
        User user = userRepository.findById(id).orElseThrow(
                () -> new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        MypageResponseDto dto = new MypageResponseDto();

        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setGender(user.getGender());
        dto.setRole(user.getRole().getRoleName().name());
        dto.setPhNum(user.getPhNum());
        dto.setBusinessNum(user.getBusinessNum());
        dto.setAdminNum(user.getAdminNum());

        return dto;
    }

    @Transactional
    public void updateUserProfile(String id, UserUpdateDto dto){
        User user = userRepository.findById(id).orElseThrow(()->
                new IllegalStateException("존재하지 않는 사용자 입니다."));

        if(dto.getPw() != null && !dto.getPw().trim().isEmpty()){ //
            //비밀번호가 null이 아닐경우와 공백이 아닐경우
            user.setPw(passwordEncoder.encode(dto.getPw()));
        }

        user.setPhNum(dto.getPhNum());
        user.setEmail(dto.getEmail());
    }
}
