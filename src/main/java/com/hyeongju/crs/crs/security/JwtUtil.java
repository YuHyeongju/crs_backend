package com.hyeongju.crs.crs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    // JWT 액세스 토큰 발급/검증 유틸 (로그인 성공 시 AuthService/AuthController에서 호출)

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    private static final long ACCESS_EXPIRY_MS = 30 * 60 * 1000L; // 30분

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(int userIdx, String role) {
        // 로그인 성공 시 발급하는 JWT 액세스 토큰 생성 (userIdx, role, 만료시각 포함)
        return Jwts.builder()
                .subject(String.valueOf(userIdx))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRY_MS))
                .signWith(getKey())
                .compact();
    }

    public boolean validateToken(String token) {
        // 서명이 유효하고 만료되지 않았는지 확인 (파싱 예외가 나면 유효하지 않은 토큰으로 간주)
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getUserIdx(String token) {
        // 토큰에서 유저 식별자(subject) 추출
        Claims claims = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
        return Integer.parseInt(claims.getSubject());
    }

    public String getRole(String token) {
        // 토큰에서 권한(role) 추출
        Claims claims = Jwts.parser().verifyWith(getKey()).build()
                .parseSignedClaims(token).getPayload();
        return (String) claims.get("role");
    }
}
