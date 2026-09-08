package com.hyeongju.crs.crs.controller;

import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.RewardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static com.hyeongju.crs.crs.controller.TestAuth.authentication;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RewardController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class RewardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RewardService rewardService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/rewards/balance/{userIdx} - 잔액 조회 성공")
    void getBalance_success() throws Exception {
        given(rewardService.getBalance(1)).willReturn(700);

        mockMvc.perform(get("/api/rewards/balance/1").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700));
    }

    @Test
    @DisplayName("GET /api/rewards/balance/{userIdx} - 인증 정보가 없으면 401")
    void getBalance_unauthorized() throws Exception {
        mockMvc.perform(get("/api/rewards/balance/1"))
                .andExpect(status().isUnauthorized());

        verify(rewardService, never()).getBalance(anyInt());
    }

    @Test
    @DisplayName("GET /api/rewards/balance/{userIdx} - 경로변수가 아닌 토큰의 userIdx 로 조회한다 (IDOR 방지)")
    void getBalance_usesAuthenticatedUserIdx() throws Exception {
        given(rewardService.getBalance(1)).willReturn(500);

        mockMvc.perform(get("/api/rewards/balance/999").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500));

        verify(rewardService).getBalance(1);
        verify(rewardService, never()).getBalance(999);
    }
}
