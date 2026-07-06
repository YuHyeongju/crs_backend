package com.hyeongju.crs.crs.config;

import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
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

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 스프링 기본 에러 포워딩 경로 - 이게 없으면 permitAll 엔드포인트에서 발생한
                        // 검증 실패 등의 예외가 /error로 내부 포워딩될 때 403으로 가려짐
                        .requestMatchers("/error").permitAll()
                        // 정적 리소스 (메뉴 이미지)
                        .requestMatchers("/uploads/**").permitAll()
                        // API 문서 (운영 환경 차단은 별도 배포 설정 작업에서 처리)
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

                        // 공개 조회 (비로그인 상태에서도 지도/상세/리뷰/혼잡도/쿠폰 목록은 보여야 함)
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/detail", "/api/restaurants/bulkDetails").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/merchant-pins", "/api/restaurants/restIdx/*",
                                "/api/restaurants/*/menus", "/api/restaurants/kakaoId/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/*").permitAll()
                        // /history가 /{kakaoId}와 세그먼트 구조가 같아 와일드카드보다 먼저 명시해야 함
                        .requestMatchers(HttpMethod.GET, "/api/congestion/history").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/congestion/*", "/api/congestion/restIdx/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/congestion/bulkStatus").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/coupons/available", "/api/coupons/available/restaurant/*").permitAll()

                        // 관리자 전용
                        .requestMatchers("/api/admins/**").hasRole("ADMIN")

                        // 그 외 전부 로그인 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
