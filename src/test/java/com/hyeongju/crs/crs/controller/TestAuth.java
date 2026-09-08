package com.hyeongju.crs.crs.controller;

import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// 컨트롤러 테스트에서 로그인 상태를 흉내내기 위한 헬퍼.
// @WebMvcTest들이 SecurityConfig 자체를 컴포넌트 스캔에서 빼고 도는 구조라(addFilters=false),
// SecurityMockMvcRequestPostProcessors.authentication()이 기대대로 동작하지 않음
// (springSecurity() MockMvc 커스터마이저가 붙지 않아 TestSecurityContextHolder → SecurityContext 연결이 안 됨).
// 대신 MockHttpServletRequest.setUserPrincipal()을 직접 세팅 - 컨트롤러의 Authentication 파라미터는
// 스프링 MVC의 PrincipalMethodArgumentResolver가 request.getUserPrincipal()을 그대로 읽어 채워주므로 동작함.
public final class TestAuth {
    private TestAuth() {}

    public static RequestPostProcessor authentication(Authentication auth) {
        return request -> {
            request.setUserPrincipal(auth);
            return request;
        };
    }
}
