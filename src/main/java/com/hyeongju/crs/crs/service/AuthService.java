package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RefreshToken;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.repository.RefreshTokenRepository;
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
    // 로그인/로그아웃, 회원가입(중복확인 포함), 리프레시 토큰 발급/갱신/삭제, 회원 탈퇴 등 인증 전반을 담당하는 서비스
    // 회원가입 자체의 필드 세팅/저장 로직은 UserService/MerchantService/AdminService(AbstractRegistrationService 상속)에
    // 위임하고, 이 클래스는 컨트롤러에서 넘어온 중복확인/인증/토큰 관련 책임만 담당함

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final MerchantService merchantService;
    private final AdminService adminService;

    // 회원가입 폼의 아이디/전화번호/사업자번호/관리자번호 중복 확인용
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
        // 일반 유저 회원가입 — 실제 생성/저장은 UserService(AbstractRegistrationService)에 위임
        userService.registerUser(dto);
    }

    @Transactional
    public void registerMerchant(MerchantRegistractionDto dto) {
        // 상인 회원가입 — 실제 생성/저장은 MerchantService(AbstractRegistrationService)에 위임
        merchantService.registerMerchant(dto);
    }

    @Transactional
    public void registerAdmin(AdminRegistractionDto dto) {
        // 관리자 회원가입 — 실제 생성/저장은 AdminService(AbstractRegistrationService)에 위임
        adminService.registerAdmin(dto);
    }

    public User authenticate(String id, String rawPassword) {
        // 로그인 검증: 아이디 존재 → 비밀번호 일치 → 탈퇴/제한 계정 여부 순으로 확인
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자 ID 입니다."));
        if (!passwordEncoder.matches(rawPassword, user.getPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        if ("WITHDRAWN".equals(user.getStatus())) {
            throw new RuntimeException("탈퇴한 계정입니다.");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("이용이 제한된 계정입니다. 관리자에게 문의하세요."); // SUSPENDED/DEACTIVATED 등
        }
        return user;
    }

    // Refresh Token 발급 (rememberMe: 30일 / 기본: 1일)
    @Transactional
    public String issueRefreshToken(int userIdx, boolean rememberMe) {
        refreshTokenRepository.deleteByUserIdx(userIdx); // 1인 1토큰 정책 - 새로 로그인하면 이전 토큰 무효화

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
        // 로그아웃 시 호출 — 토큰이 존재하면 삭제(없어도 예외 없이 넘어감)
        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Transactional
    public void withdraw(int userIdx) {
        // 회원 탈퇴 처리 — 실제 DELETE가 아니라 상태만 WITHDRAWN으로 바꾸는 소프트 삭제
        User user = userRepository.findByUserIdx(userIdx)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 사용자 입니다."));
        user.setStatus("WITHDRAWN");
        userRepository.save(user);
    }
}
