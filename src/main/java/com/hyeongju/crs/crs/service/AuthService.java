package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RefreshToken;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.repository.RefreshTokenRepository;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import com.hyeongju.crs.crs.security.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    public boolean existsById(String id) {
        return userRepository.existsById(id);
    }

    public boolean existsByPhone(String pNum) {
        return userRepository.existsByPhNum(pNum);
    }

    public boolean existsByBusinessNum(String businessNum) {
        return userRepository.existsByBusinessNum(businessNum);
    }

    public boolean existsByAdminNum(String adminNum) {
        return userRepository.existsByAdminNum(adminNum);
    }

    @Transactional
    public void registerUser(UserRegistractionDto dto) {
        String encodePassword = passwordEncoder.encode(dto.getPw());
        User user = new User();
        user.setId(dto.getId());
        user.setPw(encodePassword);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhNum(dto.getPhone());
        user.setGender(dto.getGender());
        user.setCreateTime(LocalDateTime.now());
        user.setRole(getRoleByName(RoleName.USER));
        userRepository.save(user);
    }

    @Transactional
    public void registerMerchant(MerchantRegistractionDto dto) {
        String encodePassword = passwordEncoder.encode(dto.getPw());
        User user = new User();
        user.setId(dto.getId());
        user.setPw(encodePassword);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhNum(dto.getPhone());
        user.setGender(dto.getGender());
        user.setBusinessNum(dto.getBusinessNum());
        user.setCreateTime(LocalDateTime.now());
        user.setRole(getRoleByName(RoleName.MERCHANT));
        userRepository.save(user);
    }

    @Transactional
    public void registerAdmin(AdminRegistractionDto dto) {
        String encodePassword = passwordEncoder.encode(dto.getPw());
        User user = new User();
        user.setId(dto.getId());
        user.setPw(encodePassword);
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhNum(dto.getPhone());
        user.setGender(dto.getGender());
        user.setAdminNum(dto.getAdminNum());
        user.setCreateTime(LocalDateTime.now());
        user.setRole(getRoleByName(RoleName.ADMIN));
        userRepository.save(user);
    }

    public User authenticate(String id, String rawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자 ID 입니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        if ("WITHDRAWN".equals(user.getStatus())) {
            throw new RuntimeException("탈퇴한 계정입니다.");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("이용이 제한된 계정입니다. 관리자에게 문의하세요.");
        }
        return user;
    }

    // Refresh Token 발급 (rememberMe: 30일 / 기본: 1일)
    @Transactional
    public String issueRefreshToken(int userIdx, boolean rememberMe) {
        // 기존 토큰 삭제 (1인 1토큰)
        refreshTokenRepository.deleteByUserIdx(userIdx);

        String token = UUID.randomUUID().toString();
        int days = rememberMe ? 30 : 1;

        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUserIdx(userIdx);
        rt.setExpiresAt(LocalDateTime.now().plusDays(days));
        refreshTokenRepository.save(rt);
        return token;
    }

    // Refresh Token으로 새 Access Token 발급
    @Transactional
    public Map<String, Object> refreshAccessToken(String tokenValue) {
        RefreshToken rt = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰입니다."));

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(rt);
            throw new RuntimeException("만료된 리프레시 토큰입니다.");
        }

        User user = userRepository.findByUserIdx(rt.getUserIdx())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        String role = user.getRole().getRoleName().name();
        String newAccessToken = jwtUtil.generateAccessToken(rt.getUserIdx(), role);

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", newAccessToken);
        result.put("userIdx", rt.getUserIdx());
        result.put("role", role);
        result.put("name", user.getName());
        return result;
    }

    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void withdraw(int userIdx) {
        User user = userRepository.findByUserIdx(userIdx)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자 입니다."));
        user.setStatus("WITHDRAWN");
        userRepository.save(user);
    }

    private com.hyeongju.crs.crs.domain.Role getRoleByName(RoleName roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseGet(() -> {
                    com.hyeongju.crs.crs.domain.Role newRole = new com.hyeongju.crs.crs.domain.Role();
                    newRole.setRoleName(roleName);
                    return roleRepository.save(newRole);
                });
    }
}
