package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.dto.CongestionUpdateDto;
import com.hyeongju.crs.crs.dto.MyCongestionResponseDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.CongestionService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CongestionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class CongestionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CongestionService congestionService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    @DisplayName("GET /api/congestion/{kakaoId} - 현재 혼잡도 조회")
    void getCurrentCongestion() throws Exception {
        given(congestionService.getCurrentcongestion("kakao-1")).willReturn("혼잡");

        mockMvc.perform(get("/api/congestion/kakao-1"))
                .andExpect(status().isOk())
                .andExpect(content().string("혼잡"));
    }

    @Test
    @DisplayName("GET /api/congestion/restIdx/{restIdx} - restIdx 기준 혼잡도 조회")
    void getCurrentCongestionByRestIdx() throws Exception {
        given(congestionService.getCurrentCongestionByRestIdx(10)).willReturn("여유");

        mockMvc.perform(get("/api/congestion/restIdx/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("여유"));
    }

    @Test
    @DisplayName("POST /api/congestion/bulkStatus - 여러 가게 혼잡도 일괄 조회")
    void getCurrentCongestionAll() throws Exception {
        given(congestionService.getAllCurrentCongestion(anyList()))
                .willReturn(Map.of("kakao-1", "보통", "kakao-2", "혼잡도 이력 없음"));

        mockMvc.perform(post("/api/congestion/bulkStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("kakao-1", "kakao-2"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['kakao-1']").value("보통"))
                .andExpect(jsonPath("$.['kakao-2']").value("혼잡도 이력 없음"));
    }

    @Test
    @DisplayName("POST /api/congestion/updateStatus - 제보 성공 시 200, userIdx 는 토큰 값으로 덮어쓴다")
    void updateCongestion_success() throws Exception {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setUserIdx(999); // 클라이언트 값은 무시되어야 함
        dto.setKakaoId("kakao-1");
        dto.setCongStatus("BUSY");

        mockMvc.perform(post("/api/congestion/updateStatus")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        org.mockito.ArgumentCaptor<CongestionUpdateDto> captor =
                org.mockito.ArgumentCaptor.forClass(CongestionUpdateDto.class);
        verify(congestionService).changeCongStatus(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().getUserIdx()).isEqualTo(1);
    }

    @Test
    @DisplayName("POST /api/congestion/updateStatus - 인증 정보가 없으면 401")
    void updateCongestion_unauthorized() throws Exception {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setKakaoId("kakao-1");
        dto.setCongStatus("BUSY");

        mockMvc.perform(post("/api/congestion/updateStatus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());

        verify(congestionService, never()).changeCongStatus(any(CongestionUpdateDto.class));
    }

    @Test
    @DisplayName("POST /api/congestion/updateStatus - 혼잡도 상태가 비어있으면 400")
    void updateCongestion_validationError() throws Exception {
        CongestionUpdateDto dto = new CongestionUpdateDto();
        dto.setKakaoId("kakao-1");
        dto.setCongStatus("");

        mockMvc.perform(post("/api/congestion/updateStatus")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(congestionService, never()).changeCongStatus(any(CongestionUpdateDto.class));
    }

    @Test
    @DisplayName("GET /api/congestion/history - 내 제보 이력 조회")
    void getMyHistory_success() throws Exception {
        MyCongestionResponseDto dto =
                new MyCongestionResponseDto(55, "맛있는집", "혼잡", LocalDateTime.now());
        given(congestionService.getMyCongestionHistory(1)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/congestion/history").requestAttr("authenticatedUserIdx", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restName").value("맛있는집"))
                .andExpect(jsonPath("$[0].status").value("혼잡"));
    }

    @Test
    @DisplayName("GET /api/congestion/history - 인증 정보가 없으면 401")
    void getMyHistory_unauthorized() throws Exception {
        mockMvc.perform(get("/api/congestion/history"))
                .andExpect(status().isUnauthorized());

        verify(congestionService, never()).getMyCongestionHistory(anyInt());
    }
}
