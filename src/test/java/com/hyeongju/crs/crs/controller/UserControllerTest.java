package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.dto.FindIdSendCodeDto;
import com.hyeongju.crs.crs.dto.FindIdVerifyDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.dto.ResetPasswordSendCodeDto;
import com.hyeongju.crs.crs.dto.ResetPasswordVerifyDto;
import com.hyeongju.crs.crs.dto.UserUpdateDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.UserService;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import static com.hyeongju.crs.crs.controller.TestAuth.authentication;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // ===================== 마이페이지 =====================

    @Test
    @DisplayName("GET /api/users/mypage - 인증된 사용자면 200과 프로필을 반환")
    void getUserProfile_success() throws Exception {
        MypageResponseDto dto = new MypageResponseDto(
                "testuser", "홍길동", "test@example.com", "010-1234-5678", "M", "USER", null, null);
        given(userService.getUserProfile(1)).willReturn(dto);

        mockMvc.perform(get("/api/users/mypage").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/users/mypage - 인증 정보가 없으면 401")
    void getUserProfile_unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/mypage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));
        verify(userService, never()).getUserProfile(anyInt());
    }

    @Test
    @DisplayName("GET /api/users/mypage - 사용자를 찾을 수 없으면 404")
    void getUserProfile_notFound() throws Exception {
        given(userService.getUserProfile(1)).willThrow(new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/users/mypage").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isNotFound())
                .andExpect(content().string("해당 사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("POST /api/users/mypage/updateUser - 수정 성공 시 200")
    void updateUserProfile_success() throws Exception {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setPw("NewPassw0rd!");
        dto.setEmail("new@example.com");
        dto.setPhNum("010-5555-6666");

        mockMvc.perform(post("/api/users/mypage/updateUser")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 정보가 수정되었습니다."));
    }

    @Test
    @DisplayName("POST /api/users/mypage/updateUser - 인증 정보가 없으면 401")
    void updateUserProfile_unauthorized() throws Exception {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-5555-6666");

        mockMvc.perform(post("/api/users/mypage/updateUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/users/mypage/updateUser - 이메일 형식이 잘못되면 400")
    void updateUserProfile_invalidEmail() throws Exception {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("not-an-email");
        dto.setPhNum("010-5555-6666");

        mockMvc.perform(post("/api/users/mypage/updateUser")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users/mypage/updateUser - 전화번호 형식이 잘못되면 400")
    void updateUserProfile_invalidPhone() throws Exception {
        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("01012345678");

        mockMvc.perform(post("/api/users/mypage/updateUser")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ===================== 아이디 찾기 =====================

    @Test
    @DisplayName("POST /api/users/find-id/send-code - 성공 시 200")
    void sendFindIdCode_success() throws Exception {
        FindIdSendCodeDto dto = new FindIdSendCodeDto();
        dto.setName("홍길동");
        dto.setEmail("test@example.com");

        mockMvc.perform(post("/api/users/find-id/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("인증번호가 이메일로 발송되었습니다."));

        verify(userService).sendFindIdCode("홍길동", "test@example.com");
    }

    @Test
    @DisplayName("POST /api/users/find-id/send-code - 일치하는 회원이 없으면 404")
    void sendFindIdCode_notFound() throws Exception {
        willThrow(new RuntimeException("일치하는 회원 정보가 없습니다."))
                .given(userService).sendFindIdCode(anyString(), anyString());

        FindIdSendCodeDto dto = new FindIdSendCodeDto();
        dto.setName("없음");
        dto.setEmail("none@example.com");

        mockMvc.perform(post("/api/users/find-id/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("일치하는 회원 정보가 없습니다."));
    }

    @Test
    @DisplayName("POST /api/users/find-id/send-code - 이름이 비어있으면 400")
    void sendFindIdCode_validationError() throws Exception {
        FindIdSendCodeDto dto = new FindIdSendCodeDto();
        dto.setName("");
        dto.setEmail("test@example.com");

        mockMvc.perform(post("/api/users/find-id/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users/find-id/verify - 성공 시 마스킹된 아이디 반환")
    void verifyFindId_success() throws Exception {
        given(userService.verifyFindId("홍길동", "test@example.com", "123456")).willReturn("te*****r");

        FindIdVerifyDto dto = new FindIdVerifyDto();
        dto.setName("홍길동");
        dto.setEmail("test@example.com");
        dto.setCode("123456");

        mockMvc.perform(post("/api/users/find-id/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("te*****r"));
    }

    @Test
    @DisplayName("POST /api/users/find-id/verify - 인증번호가 틀리면 400")
    void verifyFindId_invalidCode() throws Exception {
        given(userService.verifyFindId(anyString(), anyString(), anyString()))
                .willThrow(new RuntimeException("인증번호가 올바르지 않거나 만료되었습니다."));

        FindIdVerifyDto dto = new FindIdVerifyDto();
        dto.setName("홍길동");
        dto.setEmail("test@example.com");
        dto.setCode("000000");

        mockMvc.perform(post("/api/users/find-id/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("인증번호가 올바르지 않거나 만료되었습니다."));
    }

    // ===================== 비밀번호 재설정 =====================

    @Test
    @DisplayName("POST /api/users/reset-password/send-code - 성공 시 200")
    void sendResetPasswordCode_success() throws Exception {
        ResetPasswordSendCodeDto dto = new ResetPasswordSendCodeDto();
        dto.setId("testuser");
        dto.setEmail("test@example.com");

        mockMvc.perform(post("/api/users/reset-password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(userService).sendResetPasswordCode("testuser", "test@example.com");
    }

    @Test
    @DisplayName("POST /api/users/reset-password/send-code - 회원 정보가 없으면 404")
    void sendResetPasswordCode_notFound() throws Exception {
        willThrow(new RuntimeException("일치하는 회원 정보가 없습니다."))
                .given(userService).sendResetPasswordCode(anyString(), anyString());

        ResetPasswordSendCodeDto dto = new ResetPasswordSendCodeDto();
        dto.setId("nope");
        dto.setEmail("none@example.com");

        mockMvc.perform(post("/api/users/reset-password/send-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/users/reset-password/verify - 성공 시 200")
    void verifyAndResetPassword_success() throws Exception {
        ResetPasswordVerifyDto dto = new ResetPasswordVerifyDto();
        dto.setId("testuser");
        dto.setEmail("test@example.com");
        dto.setCode("123456");
        dto.setNewPassword("BrandNew1!");

        mockMvc.perform(post("/api/users/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("비밀번호가 변경되었습니다."));

        verify(userService).verifyAndResetPassword("testuser", "test@example.com", "123456", "BrandNew1!");
    }

    @Test
    @DisplayName("POST /api/users/reset-password/verify - 인증번호가 틀리면 400")
    void verifyAndResetPassword_invalidCode() throws Exception {
        willThrow(new RuntimeException("인증번호가 올바르지 않거나 만료되었습니다."))
                .given(userService).verifyAndResetPassword(anyString(), anyString(), anyString(), anyString());

        ResetPasswordVerifyDto dto = new ResetPasswordVerifyDto();
        dto.setId("testuser");
        dto.setEmail("test@example.com");
        dto.setCode("000000");
        dto.setNewPassword("BrandNew1!");

        mockMvc.perform(post("/api/users/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/users/reset-password/verify - 새 비밀번호가 정책에 맞지 않으면 400")
    void verifyAndResetPassword_weakPassword() throws Exception {
        ResetPasswordVerifyDto dto = new ResetPasswordVerifyDto();
        dto.setId("testuser");
        dto.setEmail("test@example.com");
        dto.setCode("123456");
        dto.setNewPassword("weak");

        mockMvc.perform(post("/api/users/reset-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
        verify(userService, never()).verifyAndResetPassword(anyString(), anyString(), anyString(), anyString());
    }
}
