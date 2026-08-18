package com.hyeongju.crs.crs.security;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication; // 미사용 import(이름만 같을 뿐 스프링 시큐리티와 무관)
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtTokenProvider {
    // 주의: 필드 선언과 주석뿐인 미완성 클래스. 실제 토큰 발급/검증 로직은 JwtUtil.java에 구현되어 있음

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

//    private Key key(Authentication authentication){
//        return
//    }




}
