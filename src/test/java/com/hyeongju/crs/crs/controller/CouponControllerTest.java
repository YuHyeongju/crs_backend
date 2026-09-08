package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.domain.Coupon;
import com.hyeongju.crs.crs.dto.CouponRequestDto;
import com.hyeongju.crs.crs.dto.CouponResponseDto;
import com.hyeongju.crs.crs.dto.MyCouponResponseDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.CouponService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CouponController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CouponService couponService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private CouponRequestDto requestDto() {
        CouponRequestDto dto = new CouponRequestDto();
        dto.setRestIdx(10);
        dto.setTitle("아메리카노 무료");
        dto.setDescription("설명");
        dto.setPointCost(300);
        dto.setValidUntil(LocalDate.now().plusDays(30));
        return dto;
    }

    // ===================== 등록 =====================

    @Test
    @DisplayName("POST /api/coupons/register - 등록 성공 시 200")
    void register_success() throws Exception {
        given(couponService.createCoupon(any(CouponRequestDto.class))).willReturn(new Coupon());

        mockMvc.perform(post("/api/coupons/register")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("쿠폰이 등록되었습니다."));
    }

    @Test
    @DisplayName("POST /api/coupons/register - 인증 정보가 없으면 401")
    void register_unauthorized() throws Exception {
        mockMvc.perform(post("/api/coupons/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));

        verify(couponService, never()).createCoupon(any(CouponRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/coupons/register - 본인 가게가 아니면 403")
    void register_forbidden() throws Exception {
        given(couponService.createCoupon(any(CouponRequestDto.class)))
                .willThrow(new SecurityException("본인 소유의 가게에만 쿠폰을 등록할 수 있습니다."));

        mockMvc.perform(post("/api/coupons/register")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isForbidden())
                .andExpect(content().string("본인 소유의 가게에만 쿠폰을 등록할 수 있습니다."));
    }

    @Test
    @DisplayName("POST /api/coupons/register - 포인트가 잘못되면 400")
    void register_badRequest() throws Exception {
        given(couponService.createCoupon(any(CouponRequestDto.class)))
                .willThrow(new IllegalArgumentException("필요 포인트는 1 이상이어야 합니다."));

        mockMvc.perform(post("/api/coupons/register")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/coupons/register - 가게가 없으면 404")
    void register_notFound() throws Exception {
        given(couponService.createCoupon(any(CouponRequestDto.class)))
                .willThrow(new IllegalStateException("가게를 찾을 수 없습니다."));

        mockMvc.perform(post("/api/coupons/register")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/coupons/register - 제목이 비어있으면 400 (DTO 검증)")
    void register_validationError() throws Exception {
        CouponRequestDto dto = requestDto();
        dto.setTitle("");

        mockMvc.perform(post("/api/coupons/register")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(couponService, never()).createCoupon(any(CouponRequestDto.class));
    }

    // ===================== 목록 =====================

    @Test
    @DisplayName("GET /api/coupons/my-store/{merchantUserIdx} - 내 가게 쿠폰 목록")
    void myStoreCoupons_success() throws Exception {
        CouponResponseDto dto = new CouponResponseDto(
                100, 10, "맛있는집", "아메리카노 무료", "설명", 300, LocalDate.now().plusDays(30), true);
        given(couponService.getMyStoreCoupons(1)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/coupons/my-store/1").requestAttr("authenticatedUserIdx", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].couponIdx").value(100))
                .andExpect(jsonPath("$[0].restName").value("맛있는집"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @DisplayName("GET /api/coupons/my-store/{merchantUserIdx} - 인증 정보가 없으면 401")
    void myStoreCoupons_unauthorized() throws Exception {
        mockMvc.perform(get("/api/coupons/my-store/1"))
                .andExpect(status().isUnauthorized());

        verify(couponService, never()).getMyStoreCoupons(anyInt());
    }

    @Test
    @DisplayName("GET /api/coupons/available - 교환 가능 쿠폰 목록 (비로그인 허용)")
    void available() throws Exception {
        CouponResponseDto dto = new CouponResponseDto(
                100, 10, "맛있는집", "아메리카노 무료", "설명", 300, null, true);
        given(couponService.getAvailableCoupons()).willReturn(List.of(dto));

        mockMvc.perform(get("/api/coupons/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("아메리카노 무료"))
                .andExpect(jsonPath("$[0].validUntil").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/coupons/available/restaurant/{restIdx} - 가게별 교환 가능 쿠폰")
    void availableByRestaurant() throws Exception {
        given(couponService.getAvailableCouponsByRestIdx(10)).willReturn(List.of());

        mockMvc.perform(get("/api/coupons/available/restaurant/10"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ===================== 수정/삭제 =====================

    @Test
    @DisplayName("POST /api/coupons/update/{couponIdx} - 수정 성공 시 200")
    void update_success() throws Exception {
        mockMvc.perform(post("/api/coupons/update/100")
                        .param("merchantUserIdx", "1")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(content().string("쿠폰이 수정되었습니다."));

        verify(couponService).updateCoupon(eq(100), eq(1), any(CouponRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/coupons/update/{couponIdx} - 인증 정보가 없으면 401")
    void update_unauthorized() throws Exception {
        mockMvc.perform(post("/api/coupons/update/100")
                        .param("merchantUserIdx", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/coupons/update/{couponIdx} - 본인 쿠폰이 아니면 403")
    void update_forbidden() throws Exception {
        willThrow(new SecurityException("본인 쿠폰만 수정할 수 있습니다."))
                .given(couponService).updateCoupon(anyInt(), anyInt(), any(CouponRequestDto.class));

        mockMvc.perform(post("/api/coupons/update/100")
                        .param("merchantUserIdx", "1")
                        .requestAttr("authenticatedUserIdx", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/coupons/update/{couponIdx} - 쿠폰이 없으면 404")
    void update_notFound() throws Exception {
        willThrow(new IllegalStateException("쿠폰을 찾을 수 없습니다."))
                .given(couponService).updateCoupon(anyInt(), anyInt(), any(CouponRequestDto.class));

        mockMvc.perform(post("/api/coupons/update/999")
                        .param("merchantUserIdx", "1")
                        .requestAttr("authenticatedUserIdx", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/coupons/delete/{couponIdx} - 비활성화 성공 시 200")
    void delete_success() throws Exception {
        mockMvc.perform(post("/api/coupons/delete/100")
                        .param("merchantUserIdx", "1")
                        .requestAttr("authenticatedUserIdx", 1))
                .andExpect(status().isOk())
                .andExpect(content().string("쿠폰이 삭제되었습니다."));

        verify(couponService).deactivateCoupon(100, 1);
    }

    @Test
    @DisplayName("POST /api/coupons/delete/{couponIdx} - 본인 쿠폰이 아니면 403")
    void delete_forbidden() throws Exception {
        willThrow(new SecurityException("본인 쿠폰만 삭제할 수 있습니다."))
                .given(couponService).deactivateCoupon(anyInt(), anyInt());

        mockMvc.perform(post("/api/coupons/delete/100")
                        .param("merchantUserIdx", "1")
                        .requestAttr("authenticatedUserIdx", 999))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/coupons/delete/{couponIdx} - 인증 정보가 없으면 401")
    void delete_unauthorized() throws Exception {
        mockMvc.perform(post("/api/coupons/delete/100").param("merchantUserIdx", "1"))
                .andExpect(status().isUnauthorized());
    }

    // ===================== 교환/보유/사용 =====================

    @Test
    @DisplayName("POST /api/coupons/{couponIdx}/redeem - 교환 성공 시 200")
    void redeem_success() throws Exception {
        mockMvc.perform(post("/api/coupons/100/redeem")
                        .param("userIdx", "2")
                        .requestAttr("authenticatedUserIdx", 2))
                .andExpect(status().isOk())
                .andExpect(content().string("쿠폰을 교환했습니다."));

        verify(couponService).redeemCoupon(100, 2);
    }

    @Test
    @DisplayName("POST /api/coupons/{couponIdx}/redeem - 포인트가 부족하면 400")
    void redeem_insufficientPoints() throws Exception {
        willThrow(new IllegalArgumentException("포인트가 부족합니다. (보유 100P / 필요 300P)"))
                .given(couponService).redeemCoupon(anyInt(), anyInt());

        mockMvc.perform(post("/api/coupons/100/redeem")
                        .param("userIdx", "2")
                        .requestAttr("authenticatedUserIdx", 2))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("포인트가 부족합니다. (보유 100P / 필요 300P)"));
    }

    @Test
    @DisplayName("POST /api/coupons/{couponIdx}/redeem - 유효기간이 지났으면 404")
    void redeem_expired() throws Exception {
        willThrow(new IllegalStateException("유효기간이 지난 쿠폰입니다."))
                .given(couponService).redeemCoupon(anyInt(), anyInt());

        mockMvc.perform(post("/api/coupons/100/redeem")
                        .param("userIdx", "2")
                        .requestAttr("authenticatedUserIdx", 2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/coupons/{couponIdx}/redeem - 인증 정보가 없으면 401")
    void redeem_unauthorized() throws Exception {
        mockMvc.perform(post("/api/coupons/100/redeem").param("userIdx", "2"))
                .andExpect(status().isUnauthorized());

        verify(couponService, never()).redeemCoupon(anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /api/coupons/my/{userIdx} - 보유 쿠폰 목록")
    void myCoupons_success() throws Exception {
        MyCouponResponseDto dto = new MyCouponResponseDto(
                7, "아메리카노 무료", "맛있는집", 300, LocalDate.now().plusDays(10),
                false, LocalDateTime.now(), null);
        given(couponService.getMyCoupons(2)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/coupons/my/2").requestAttr("authenticatedUserIdx", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userCouponIdx").value(7))
                .andExpect(jsonPath("$[0].used").value(false));
    }

    @Test
    @DisplayName("GET /api/coupons/my/{userIdx} - 인증 정보가 없으면 401")
    void myCoupons_unauthorized() throws Exception {
        mockMvc.perform(get("/api/coupons/my/2"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/coupons/use/{userCouponIdx} - 사용 성공 시 200")
    void use_success() throws Exception {
        mockMvc.perform(post("/api/coupons/use/7")
                        .param("userIdx", "2")
                        .requestAttr("authenticatedUserIdx", 2))
                .andExpect(status().isOk())
                .andExpect(content().string("쿠폰을 사용했습니다."));

        verify(couponService).useCoupon(7, 2);
    }

    @Test
    @DisplayName("POST /api/coupons/use/{userCouponIdx} - 본인 쿠폰이 아니면 403")
    void use_forbidden() throws Exception {
        willThrow(new SecurityException("본인 쿠폰만 사용할 수 있습니다."))
                .given(couponService).useCoupon(anyInt(), anyInt());

        mockMvc.perform(post("/api/coupons/use/7")
                        .param("userIdx", "2")
                        .requestAttr("authenticatedUserIdx", 999))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/coupons/use/{userCouponIdx} - 이미 사용한 쿠폰이면 409")
    void use_conflict() throws Exception {
        willThrow(new IllegalStateException("이미 사용한 쿠폰입니다."))
                .given(couponService).useCoupon(anyInt(), anyInt());

        mockMvc.perform(post("/api/coupons/use/7")
                        .param("userIdx", "2")
                        .requestAttr("authenticatedUserIdx", 2))
                .andExpect(status().isConflict())
                .andExpect(content().string("이미 사용한 쿠폰입니다."));
    }

    @Test
    @DisplayName("POST /api/coupons/use/{userCouponIdx} - 인증 정보가 없으면 401")
    void use_unauthorized() throws Exception {
        mockMvc.perform(post("/api/coupons/use/7").param("userIdx", "2"))
                .andExpect(status().isUnauthorized());
    }
}
