package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
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
        User newUser = super.registerCommonFields(dto, RoleName.Role_USER);

        return userRepository.save(newUser);
    }

}
