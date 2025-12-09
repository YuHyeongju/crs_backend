package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.BaseRegistrationDto;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@RequiredArgsConstructor

public abstract class AbstractRegistrationService {

    protected final UserRepository userRepository;
    protected final RoleRepository roleRepository;
    protected final PasswordEncoder passwordEncoder;

    protected User registerCommonFields(BaseRegistrationDto dto, RoleName roleName) {

        // 1. 공통 유효성 검증
        if (userRepository.existsById(dto.getId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (!dto.getPw().equals(dto.getConfirmPw())) {
            throw new IllegalArgumentException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        // 2. 역할(Role) 조회
        Role defaultRole = roleRepository.findByRoleName(roleName.name())
                .orElseThrow(() -> new IllegalStateException("기본 역할(" + roleName.name() + ")이 DB에 존재하지 않습니다."));

        // 3. 비밀번호 원문을 가져다가 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPw());

        User newUser = new User();
        // DTO의 공통 필드
        newUser.setId(dto.getId());
        newUser.setPw(encodedPassword);
        newUser.setEmail(dto.getEmail());
        newUser.setName(dto.getName());
        newUser.setPNum(dto.getPhone());
        newUser.setGender(dto.getGender());

        // 서버에서 생성한 데이터
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setRole(defaultRole);

        // 반환
        return newUser;
    }
}
