package com.hyeongju.crs.crs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // 모든 HTTP 요청마다 실행되어 Authorization 헤더의 JWT를 검사하고,
    // 유효하면 로그인 유저 정보를 request/SecurityContext에 심어주는 필터

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validateToken(token)) {
                int userIdx = jwtUtil.getUserIdx(token);
                String role = jwtUtil.getRole(token);

                request.setAttribute("authenticatedUserIdx", userIdx);

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userIdx, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                // SecurityConfig의 hasRole("ADMIN") 같은 인가 규칙이 이 인증 정보를 보고 판단함
            }
            // 토큰이 유효하지 않아도 예외 없이 인증 정보 없는 익명 요청으로 통과시킴
        }
        chain.doFilter(request, response);
    }
}
