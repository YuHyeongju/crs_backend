package com.hyeongju.crs.crs.security;

import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
public class JwtTokenProvider {

    // application.properties에 있는 시크릿 키 주입
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    // application.properties에 있는 토큰 만료 시간 주입
    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    //  서명에 사용할 키 객체 생성
    //  내가 사이트에서 만들어 온 시크릿 키를 디코딩 하는 메서드
//    private Key key(Authentication authentication){
//        return
//    }




}
