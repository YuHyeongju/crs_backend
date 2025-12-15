package com.hyeongju.crs.crs.config;

import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity // 스프링 시큐리티 설정 활성화
@RequiredArgsConstructor // JWT 필터 주입
public class SecurityConfig  {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(); // 비밀번호 암호화 표준 알고리즘
    }

    @Bean // AuthenticationManager를 빈으로 등록함.
    // AuthenticationManager는 사용자의 자격증명을 검증하고 성공하면 Authentication 객체를 반환함.
    // AuthenticationConfiguration은 인증 설정을 관리하고 구성하는데 사용하는 객체
    // 로그인시에 ID/PW 검증에 이용
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
    throws Exception{
        return authenticationConfiguration.getAuthenticationManager();
    }
    // 보안 필터 설정
    @Bean
    public SecurityFilterChain FilterChain(HttpSecurity http) throws Exception{
        
        // 정책 설정
        
        http.csrf(AbstractHttpConfigurer::disable); //csrf 비활성화
        // AbstractHttpConfigurer의 disable() 정적 메서드를 참조함.
        // http.csrf(csrf ->csrf.disable());
        http.cors(AbstractHttpConfigurer:: disable);

        http.sessionManagement(session -> session.
                sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        // JWT 기반이라 세션을 사용하지 않음.

        //인가(Authorization) 설정 경로별 접근 권한 정의
        http.authorizeHttpRequests(authorize -> authorize.
                requestMatchers("api/*/register",  // 이 주소는 추후에 변경해야함.
                        "api/auth/login").permitAll()
                // 회원가입 페이지나 로그인 페이지는 누구나 접근 허용
                //.anyRequest().authenticated()
                // 나머지 페이지는 로그인 요구
                // 지금은 로그인만 구현하면 되서 일단 비활성화
        );

        // 커스텀 필터 등록
//        http.addFilterBefore(
//                jwtAuthenticationFilter, //  지금 빈 껍데이라 구현 필요
//                UsernamePasswordAuthenticationFilter.class
//        );


        return http.build();
    }

}
