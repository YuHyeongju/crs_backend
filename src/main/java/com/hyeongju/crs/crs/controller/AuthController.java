package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserLoginDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.security.JwtUtil;
import com.hyeongju.crs.crs.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @PostMapping("/register/user")
    public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegistractionDto dto) {
        if (authService.existsById(dto.getId()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 ID 입니다.");
        if (authService.existsByPhone(dto.getPhone()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 휴대폰 번호 입니다.");
        if (!dto.getPw().equals(dto.getConfirmPw()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        authService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
    }

    @PostMapping("/register/merchant")
    public ResponseEntity<String> registerMerchant(@Valid @RequestBody MerchantRegistractionDto dto) {
        if (authService.existsById(dto.getId()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 ID 입니다.");
        if (authService.existsByPhone(dto.getPhone()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 휴대폰 번호 입니다.");
        if (!dto.getPw().equals(dto.getConfirmPw()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        if (authService.existsByBusinessNum(dto.getBusinessNum()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 등록된 사업자 등록 번호 입니다.");
        authService.registerMerchant(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("상인 회원가입 성공");
    }

    @PostMapping("/register/admin")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody AdminRegistractionDto dto) {
        if (authService.existsById(dto.getId()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 ID입니다.");
        if (authService.existsByPhone(dto.getPhone()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 존재하는 휴대폰 번호 입니다.");
        if (!dto.getPw().equals(dto.getConfirmPw()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        if (authService.existsByAdminNum(dto.getAdminNum()))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 등록된 관리자 코드 입니다.");
        authService.registerAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("관리자 회원가입 성공");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginDto dto, HttpServletResponse response) {
        try {
            User user = authService.authenticate(dto.getId(), dto.getPw());
            String role = user.getRole().getRoleName().name();

            String accessToken = jwtUtil.generateAccessToken(user.getUserIdx(), role);
            String refreshToken = authService.issueRefreshToken(user.getUserIdx(), dto.isRememberMe());

            // Refresh Token → HttpOnly 쿠키
            // rememberMe=true: 30일 영구 쿠키 / false: 세션 쿠키 (브라우저 닫으면 삭제)
            ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .sameSite("Lax");
            if (dto.isRememberMe()) {
                cookieBuilder.maxAge(30 * 24 * 60 * 60);
            }
            ResponseCookie cookie = cookieBuilder.build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            Map<String, Object> body = new HashMap<>();
            body.put("accessToken", accessToken);
            body.put("userIdx", user.getUserIdx());
            body.put("role", role);
            body.put("name", user.getName());
            return ResponseEntity.ok(body);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("리프레시 토큰이 없습니다.");
        try {
            Map<String, Object> result = authService.refreshAccessToken(refreshToken);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            authService.deleteRefreshToken(refreshToken);
        }
        ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/").maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(
            HttpServletRequest request,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        try {
            Integer authedUserIdx = (Integer) request.getAttribute("authenticatedUserIdx");
            if (authedUserIdx == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
            }
            authService.withdraw(authedUserIdx);
            if (refreshToken != null) authService.deleteRefreshToken(refreshToken);
            ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true).secure(cookieSecure).sameSite("Lax").path("/").maxAge(0).build();
            response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
            return ResponseEntity.ok("회원 탈퇴가 완료 되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }
}
