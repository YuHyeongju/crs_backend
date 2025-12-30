package com.hyeongju.crs.crs.service;


import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService extends AbstractRegistrationService {

    public AdminService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ){
        super(userRepository,roleRepository,passwordEncoder);
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
}
