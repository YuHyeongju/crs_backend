package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BaseRegistrationDto;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@RequiredArgsConstructor

public abstract class AbstractRegistrationService {
    // 일반유저/상인/관리자 3종의 회원가입 서비스가 공통으로 쓰는 로직을 모아둔 추상 클래스
    // UserService, MerchantService, AdminService가 이 클래스를 상속받아 사용함

    protected final UserRepository userRepository;
    protected final RoleRepository roleRepository;
    protected final PasswordEncoder passwordEncoder;

    @Transactional
    protected User registerCommonFields(BaseRegistrationDto dto, RoleName roleName) {
        // 회원 유형에 상관없이 공통되는 필드(아이디/비번/이름/이메일 등)로 User 엔티티를 만들어 반환

        if (userRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (!dto.getPw().equals(dto.getConfirmPw())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        Role defaultRole = roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                        Role newRole = new Role();
                        newRole.setRoleName(roleName);
                  return roleRepository.save(newRole);
                }); // Role 테이블에 roleName이 없으면 생성해서 저장

        String encodedPassword = passwordEncoder.encode(dto.getPw());

        User newUser = new User();
        newUser.setId(dto.getId());
        newUser.setPw(encodedPassword);
        newUser.setEmail(dto.getEmail());
        newUser.setName(dto.getName());
        newUser.setPhNum(dto.getPhone());
        newUser.setGender(dto.getGender());

        newUser.setCreateTime(LocalDateTime.now());
        newUser.setRole(defaultRole);

        // DB 저장은 각 하위 서비스가 유형별 필드까지 채운 뒤 직접 수행 (여기서는 save 호출 안 함)
        return newUser;
    }
}
