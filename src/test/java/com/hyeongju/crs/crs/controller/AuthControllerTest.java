package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserLoginDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.security.JwtUtil;
import com.hyeongju.crs.crs.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UserRegistractionDto userDto() {
        UserRegistractionDto dto = new UserRegistractionDto();
        dto.setId("testuser");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("홍길동");
        dto.setEmail("test@example.com");
        dto.setPhone("010-1234-5678");
        dto.setGender("M");
        return dto;
    }

    private MerchantRegistractionDto merchantDto() {
        MerchantRegistractionDto dto = new MerchantRegistractionDto();
        dto.setId("merchant1");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("사장님");
        dto.setEmail("m@example.com");
        dto.setPhone("010-2222-3333");
        dto.setGender("M");
        dto.setBusinessNum("123-45-67890");
        return dto;
    }

    private AdminRegistractionDto adminDto() {
        AdminRegistractionDto dto = new AdminRegistractionDto();
        dto.setId("adminuser");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("관리자");
        dto.setEmail("admin@example.com");
        dto.setPhone("010-3333-4444");
        dto.setGender("F");
        dto.setAdminNum("ABC1234");
        return dto;
    }

    // ===================== 회원가입 =====================

    @Test
    @DisplayName("POST /api/auth/register/user - 회원가입 성공 시 201")
    void registerUser_success() throws Exception {
        given(authService.existsById("testuser")).willReturn(false);
        given(authService.existsByPhone("010-1234-5678")).willReturn(false);

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto())))
                .andExpect(status().isCreated())
                .andExpect(content().string("회원가입 성공"));

        verify(authService).registerUser(any(UserRegistractionDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/register/user - 아이디가 중복이면 400")
    void registerUser_duplicateId() throws Exception {
        given(authService.existsById("testuser")).willReturn(true);

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("이미 존재하는 ID 입니다."));

        verify(authService, never()).registerUser(any(UserRegistractionDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/register/user - 전화번호가 중복이면 400")
    void registerUser_duplicatePhone() throws Exception {
        given(authService.existsById("testuser")).willReturn(false);
        given(authService.existsByPhone("010-1234-5678")).willReturn(true);

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("이미 존재하는 휴대폰 번호 입니다."));
    }

    @Test
    @DisplayName("POST /api/auth/register/user - 비밀번호 확인이 다르면 400")
    void registerUser_passwordMismatch() throws Exception {
        UserRegistractionDto dto = userDto();
        dto.setConfirmPw("Different1!");
        given(authService.existsById("testuser")).willReturn(false);
        given(authService.existsByPhone("010-1234-5678")).willReturn(false);

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("비밀번호와 비밀번호 확인이 일치하지 않습니다."));
    }

    @Test
    @DisplayName("POST /api/auth/register/user - 비밀번호 정책 위반이면 400 (DTO 검증)")
    void registerUser_validationError() throws Exception {
        UserRegistractionDto dto = userDto();
        dto.setPw("weak");
        dto.setConfirmPw("weak");

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(authService, never()).registerUser(any(UserRegistractionDto.class));
    }

    @Test
    @DisplayName("POST /api/auth/register/user - 전화번호 형식이 잘못되면 400 (DTO 검증)")
    void registerUser_invalidPhoneFormat() throws Exception {
        UserRegistractionDto dto = userDto();
        dto.setPhone("01012345678");

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/register/merchant - 회원가입 성공 시 201")
    void registerMerchant_success() throws Exception {
        given(authService.existsById(anyString())).willReturn(false);
        given(authService.existsByPhone(anyString())).willReturn(false);
        given(authService.existsByBusinessNum(anyString())).willReturn(false);

        mockMvc.perform(post("/api/auth/register/merchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantDto())))
                .andExpect(status().isCreated())
                .andExpect(content().string("상인 회원가입 성공"));
    }

    @Test
    @DisplayName("POST /api/auth/register/merchant - 사업자 번호가 중복이면 400")
    void registerMerchant_duplicateBusinessNum() throws Exception {
        given(authService.existsById(anyString())).willReturn(false);
        given(authService.existsByPhone(anyString())).willReturn(false);
        given(authService.existsByBusinessNum("123-45-67890")).willReturn(true);

        mockMvc.perform(post("/api/auth/register/merchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(merchantDto())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("이미 등록된 사업자 등록 번호 입니다."));
    }

    @Test
    @DisplayName("POST /api/auth/register/admin - 회원가입 성공 시 201")
    void registerAdmin_success() throws Exception {
        given(authService.existsById(anyString())).willReturn(false);
        given(authService.existsByPhone(anyString())).willReturn(false);
        given(authService.existsByAdminNum(anyString())).willReturn(false);

        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDto())))
                .andExpect(status().isCreated())
                .andExpect(content().string("관리자 회원가입 성공"));
    }

    @Test
    @DisplayName("POST /api/auth/register/admin - 관리자 코드가 중복이면 400")
    void registerAdmin_duplicateAdminNum() throws Exception {
        given(authService.existsById(anyString())).willReturn(false);
        given(authService.existsByPhone(anyString())).willReturn(false);
        given(authService.existsByAdminNum("ABC1234")).willReturn(true);

        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminDto())))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("이미 등록된 관리자 코드 입니다."));
    }

    // ===================== 로그인 =====================

    private User loginUser() {
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        User user = new User();
        user.setUserIdx(1);
        user.setId("testuser");
        user.setName("홍길동");
        user.setStatus("ACTIVE");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("POST /api/auth/login - 로그인 성공 시 토큰과 리프레시 쿠키를 반환")
    void login_success() throws Exception {
        given(authService.authenticate("testuser", "Passw0rd!")).willReturn(loginUser());
        given(jwtUtil.generateAccessToken(1, "USER")).willReturn("access-token");
        given(authService.issueRefreshToken(1, false)).willReturn("refresh-token");

        UserLoginDto dto = new UserLoginDto();
        dto.setId("testuser");
        dto.setPw("Passw0rd!");
        dto.setRememberMe(false);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.userIdx").value(1))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(cookie().value("refreshToken", "refresh-token"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    @DisplayName("POST /api/auth/login - rememberMe=true 면 30일 만료 쿠키가 내려간다")
    void login_rememberMe() throws Exception {
        given(authService.authenticate(anyString(), anyString())).willReturn(loginUser());
        given(jwtUtil.generateAccessToken(anyInt(), anyString())).willReturn("access-token");
        given(authService.issueRefreshToken(anyInt(), anyBoolean())).willReturn("refresh-token");

        UserLoginDto dto = new UserLoginDto();
        dto.setId("testuser");
        dto.setPw("Passw0rd!");
        dto.setRememberMe(true);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 30 * 24 * 60 * 60));
    }

    @Test
    @DisplayName("POST /api/auth/login - 인증 실패 시 401")
    void login_failure() throws Exception {
        given(authService.authenticate(anyString(), anyString()))
                .willThrow(new RuntimeException("비밀번호가 일치하지 않습니다."));

        UserLoginDto dto = new UserLoginDto();
        dto.setId("testuser");
        dto.setPw("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("비밀번호가 일치하지 않습니다."));
    }

    @Test
    @DisplayName("POST /api/auth/login - 아이디가 비어있으면 400")
    void login_validationError() throws Exception {
        UserLoginDto dto = new UserLoginDto();
        dto.setId("");
        dto.setPw("Passw0rd!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ===================== 토큰 재발급 / 로그아웃 / 탈퇴 =====================

    @Test
    @DisplayName("POST /api/auth/refresh - 쿠키가 없으면 401")
    void refresh_noCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("리프레시 토큰이 없습니다."));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - 성공 시 새 액세스 토큰 반환")
    void refresh_success() throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", "new-access-token");
        result.put("userIdx", 1);
        result.put("role", "USER");
        result.put("name", "홍길동");
        given(authService.refreshAccessToken("refresh-token")).willReturn(result);

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - 만료된 토큰이면 401")
    void refresh_expired() throws Exception {
        given(authService.refreshAccessToken(anyString()))
                .willThrow(new RuntimeException("만료된 리프레시 토큰입니다."));

        mockMvc.perform(post("/api/auth/refresh").cookie(new Cookie("refreshToken", "expired")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("만료된 리프레시 토큰입니다."));
    }

    @Test
    @DisplayName("POST /api/auth/logout - 토큰 삭제 후 쿠키를 만료시킨다")
    void logout_withCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 되었습니다."))
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(authService).deleteRefreshToken("refresh-token");
    }

    @Test
    @DisplayName("POST /api/auth/logout - 쿠키가 없어도 200")
    void logout_withoutCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());

        verify(authService, never()).deleteRefreshToken(anyString());
    }

    @Test
    @DisplayName("POST /api/auth/withdraw - 인증 정보가 없으면 401")
    void withdraw_unauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/withdraw"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));

        verify(authService, never()).withdraw(anyInt());
    }

    @Test
    @DisplayName("POST /api/auth/withdraw - 성공 시 200 및 쿠키 만료")
    void withdraw_success() throws Exception {
        mockMvc.perform(post("/api/auth/withdraw")
                        .requestAttr("authenticatedUserIdx", 1)
                        .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 탈퇴가 완료 되었습니다."))
                .andExpect(cookie().maxAge("refreshToken", 0));

        verify(authService).withdraw(1);
        verify(authService).deleteRefreshToken("refresh-token");
    }
}
