package com.hyeongju.crs.crs.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 request 속성과 SecurityContext 에 인증 정보를 심는다")
    void validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        given(jwtUtil.validateToken("valid-token")).willReturn(true);
        given(jwtUtil.getUserIdx("valid-token")).willReturn(7);
        given(jwtUtil.getRole("valid-token")).willReturn("ADMIN");

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("authenticatedUserIdx")).isEqualTo(7);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(7);
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
        // 필터 체인은 항상 이어져야 한다
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 인증 없이 통과시킨다")
    void invalidToken_passesThroughAnonymously() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        given(jwtUtil.validateToken("bad-token")).willReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("authenticatedUserIdx")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
        verify(jwtUtil, never()).getUserIdx("bad-token");
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 토큰 검사를 하지 않는다")
    void noHeader_skipsTokenCheck() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("authenticatedUserIdx")).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("Bearer 스킴이 아닌 헤더는 무시한다")
    void nonBearerHeader_isIgnored() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("USER 역할 토큰은 ROLE_USER 권한으로 매핑된다")
    void userRole_mapsToRoleUser() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer user-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        given(jwtUtil.validateToken("user-token")).willReturn(true);
        given(jwtUtil.getUserIdx("user-token")).willReturn(1);
        given(jwtUtil.getRole("user-token")).willReturn("USER");

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_USER");
    }
}
