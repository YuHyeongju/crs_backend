package com.hyeongju.crs.crs.config;

import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // 인증/인가 규칙, CORS, 비밀번호 암호화 방식을 정의하는 보안 설정

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 회원가입/로그인 시 비밀번호를 단방향 해시로 암호화하는 데 쓰는 인코더
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // HTTP 요청별 인가 규칙(permitAll/authenticated/hasRole) + CORS + 세션 정책 + JWT 필터 등록을 구성
        http
                .csrf(csrf -> csrf.disable())
                // JWT 기반 API 서버는 쿠키 자동 전송에 의존하는 CSRF 공격 벡터가 사실상 없어 비활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 세션 미사용, 매 요청을 JWT로만 인증
                .authorizeHttpRequests(auth -> auth
                        // permitAll 엔드포인트의 예외가 /error로 내부 포워딩될 때 403으로 가려지는 것을 방지
                        .requestMatchers("/error").permitAll()
                        // 정적 리소스 (메뉴 이미지)
                        .requestMatchers("/uploads/**").permitAll()
                        // API 문서 (운영 환경 차단은 별도 배포 설정에서 처리)
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/v3/api-docs.yaml").permitAll()

                        // 회원가입 / 로그인 / 토큰 재발급 / 로그아웃 / 아이디찾기 / 비밀번호 재설정
                        .requestMatchers("/api/auth/register/**").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/users/find-id/**").permitAll()
                        .requestMatchers("/api/users/reset-password/**").permitAll()

                        // 공개 조회 (비로그인 상태에서도 지도/상세/리뷰/혼잡도/쿠폰 목록은 노출되어야 함)
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/detail", "/api/restaurants/bulkDetails").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/merchant-pins", "/api/restaurants/restIdx/*",
                                "/api/restaurants/*/menus", "/api/restaurants/kakaoId/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/*").permitAll()
                        // /history가 /{kakaoId}와 세그먼트 구조가 같아 와일드카드 규칙보다 먼저 명시(매처는 순서대로 평가됨)
                        .requestMatchers(HttpMethod.GET, "/api/congestion/history").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/congestion/*", "/api/congestion/restIdx/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/congestion/bulkStatus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/available", "/api/coupons/available/restaurant/*").permitAll()

                        // 관리자 전용 (JwtAuthenticationFilter가 심어준 ROLE_ADMIN 권한 필요)
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                // 스프링 시큐리티 기본 폼로그인 필터보다 먼저 JWT 필터가 실행되도록 순서 지정
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 프론트엔드 오리진에서의 크로스 오리진 요청을 허용하기 위한 CORS 설정
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // 리프레시 토큰 쿠키를 크로스 오리진 요청에도 실어 보내기 위함

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
