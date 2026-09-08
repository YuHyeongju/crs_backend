package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.dto.MerchantUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.MerchantService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MerchantController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MerchantService merchantService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/merchants/mypage - 성공 시 200")
    void getMerchantProfile_success() throws Exception {
        MypageResponseDto dto = new MypageResponseDto(
                "merchant1", "사장님", "m@example.com", "010-2222-3333", "M", "MERCHANT", "1234567890", null);
        given(merchantService.getMerchantProfile(1)).willReturn(dto);

        mockMvc.perform(get("/api/merchants/mypage").requestAttr("authenticatedUserIdx", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("merchant1"))
                .andExpect(jsonPath("$.role").value("MERCHANT"))
                .andExpect(jsonPath("$.businessNum").value("1234567890"));
    }

    @Test
    @DisplayName("GET /api/merchants/mypage - 인증 정보가 없으면 401")
    void getMerchantProfile_unauthorized() throws Exception {
        mockMvc.perform(get("/api/merchants/mypage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 만료되었습니다."));

        verify(merchantService, never()).getMerchantProfile(anyInt());
    }

    @Test
    @DisplayName("GET /api/merchants/mypage - 사용자를 찾을 수 없으면 404")
    void getMerchantProfile_notFound() throws Exception {
        given(merchantService.getMerchantProfile(1))
                .willThrow(new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/merchants/mypage").requestAttr("authenticatedUserIdx", 1))
                .andExpect(status().isNotFound())
                .andExpect(content().string("해당 사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("POST /api/merchants/mypage/updateMerchant - 수정 성공 시 200")
    void updateMerchantProfile_success() throws Exception {
        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-0000");
        dto.setBusinessNum("9876543210");

        mockMvc.perform(post("/api/merchants/mypage/updateMerchant")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 정보가 수정되었습니다."));
    }

    @Test
    @DisplayName("POST /api/merchants/mypage/updateMerchant - 인증 정보가 없으면 401")
    void updateMerchantProfile_unauthorized() throws Exception {
        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-0000");
        dto.setBusinessNum("9876543210");

        mockMvc.perform(post("/api/merchants/mypage/updateMerchant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("POST /api/merchants/mypage/updateMerchant - 사업자 번호가 비어있으면 400")
    void updateMerchantProfile_validationError() throws Exception {
        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-0000");
        dto.setBusinessNum("");

        mockMvc.perform(post("/api/merchants/mypage/updateMerchant")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/merchants/mypage/updateMerchant - 이메일 형식이 잘못되면 400")
    void updateMerchantProfile_invalidEmail() throws Exception {
        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setEmail("not-an-email");
        dto.setPhNum("010-9999-0000");
        dto.setBusinessNum("9876543210");

        mockMvc.perform(post("/api/merchants/mypage/updateMerchant")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }
}
