package com.hyeongju.crs.crs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-jwt-secret-key-for-unit-tests-1234567890";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // @Value 로 주입되는 비밀키를 테스트용 값으로 직접 세팅
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", SECRET);
    }

    private SecretKey key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("발급한 토큰은 유효하며 userIdx / role 을 그대로 담고 있다")
    void generateAndParseAccessToken() {
        String token = jwtUtil.generateAccessToken(42, "MERCHANT");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.getUserIdx(token)).isEqualTo(42);
        assertThat(jwtUtil.getRole(token)).isEqualTo("MERCHANT");
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 유효하지 않다")
    void validateToken_malformed() {
        assertThat(jwtUtil.validateToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 토큰은 유효하지 않다")
    void validateToken_empty() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰은 유효하지 않다")
    void validateToken_wrongSignature() {
        String foreignToken = Jwts.builder()
                .subject("1")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key("completely-different-secret-key-0987654321-abcdef"))
                .compact();

        assertThat(jwtUtil.validateToken(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 유효하지 않다")
    void validateToken_expired() {
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("role", "USER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key(SECRET))
                .compact();

        assertThat(jwtUtil.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("역할별로 토큰을 발급해도 각각의 role 이 올바르게 추출된다")
    void generateAccessToken_perRole() {
        assertThat(jwtUtil.getRole(jwtUtil.generateAccessToken(1, "USER"))).isEqualTo("USER");
        assertThat(jwtUtil.getRole(jwtUtil.generateAccessToken(2, "ADMIN"))).isEqualTo("ADMIN");
        assertThat(jwtUtil.getUserIdx(jwtUtil.generateAccessToken(999, "USER"))).isEqualTo(999);
    }
}
