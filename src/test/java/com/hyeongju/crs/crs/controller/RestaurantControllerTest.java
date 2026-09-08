package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.MenuResponseDto;
import com.hyeongju.crs.crs.dto.RestaurantPinDto;
import com.hyeongju.crs.crs.dto.RestaurantRequestDto;
import com.hyeongju.crs.crs.dto.RestaurantResponseDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.RestaurantService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.TestingAuthenticationToken;
import static com.hyeongju.crs.crs.controller.TestAuth.authentication;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RestaurantController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestaurantService restaurantService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private Restaurant sampleRestaurant() {
        Restaurant r = new Restaurant();
        r.setRestIdx(10);
        r.setKakaoId("kakao-1");
        r.setRestName("맛있는집");
        r.setRestAddress("서울시 강남구");
        r.setRestTel("02-123-4567");
        return r;
    }

    private RestaurantRequestDto requestDto() {
        RestaurantRequestDto dto = new RestaurantRequestDto();
        dto.setKakaoId("kakao-1");
        dto.setRestName("맛있는집");
        dto.setRestAddress("서울시 강남구");
        dto.setRestTel("02-123-4567");
        dto.setRestBusiHours("10:00 - 22:00");
        return dto;
    }

    private MockMultipartFile dtoPart() throws Exception {
        return new MockMultipartFile("dto", "dto.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(requestDto()).getBytes(StandardCharsets.UTF_8));
    }

    // ===================== 상세/생성 =====================

    @Test
    @DisplayName("POST /api/restaurants/detail - 카카오 정보 기준 조회/생성")
    void restaurantDetail() throws Exception {
        given(restaurantService.getOrCreateRestaurant(anyString(), anyString(), anyString(), anyString()))
                .willReturn(sampleRestaurant());

        mockMvc.perform(post("/api/restaurants/detail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restIdx").value(10))
                .andExpect(jsonPath("$.restName").value("맛있는집"));
    }

    // ===================== 등록 (멀티파트) =====================

    @Test
    @DisplayName("POST /api/restaurants/register - 등록 성공 시 200")
    void registerRestaurant_success() throws Exception {
        given(restaurantService.registerRestaurantByMerchant(
                any(RestaurantRequestDto.class), eq(1), any())).willReturn(sampleRestaurant());

        mockMvc.perform(multipart("/api/restaurants/register")
                        .file(dtoPart())
                        .with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restIdx").value(10));
    }

    @Test
    @DisplayName("POST /api/restaurants/register - 인증 정보가 없으면 401")
    void registerRestaurant_unauthorized() throws Exception {
        mockMvc.perform(multipart("/api/restaurants/register").file(dtoPart()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("POST /api/restaurants/register - 이미 등록된 가게면 409")
    void registerRestaurant_conflict() throws Exception {
        given(restaurantService.registerRestaurantByMerchant(
                any(RestaurantRequestDto.class), anyInt(), any()))
                .willThrow(new IllegalStateException("이미 다른 사업자가 등록한 가게입니다."));

        mockMvc.perform(multipart("/api/restaurants/register")
                        .file(dtoPart())
                        .with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isConflict())
                .andExpect(content().string("이미 다른 사업자가 등록한 가게입니다."));
    }

    @Test
    @DisplayName("POST /api/restaurants/register - 메뉴 이름이 비어있으면 400 (DTO 검증)")
    void registerRestaurant_validationError() throws Exception {
        RestaurantRequestDto dto = requestDto();
        RestaurantRequestDto.MenuList menu = new RestaurantRequestDto.MenuList();
        menu.setMenuName("");
        menu.setMenuPrice(1000);
        dto.setMenulist(List.of(menu));

        MockMultipartFile part = new MockMultipartFile("dto", "dto.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsString(dto).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/restaurants/register")
                        .file(part)
                        .with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isBadRequest());
    }

    // ===================== 목록/핀/메뉴 =====================

    @Test
    @DisplayName("GET /api/restaurants/my-restaurant-list - 내 가게 목록")
    void getMyRestaurants_success() throws Exception {
        RestaurantResponseDto dto = new RestaurantResponseDto(10, "맛있는집", "서울시 강남구", 4.3, 3, 1);
        given(restaurantService.getMyRestaurants(1)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/restaurants/my-restaurant-list").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restIdx").value(10))
                .andExpect(jsonPath("$[0].averageRating").value(4.3));
    }

    @Test
    @DisplayName("GET /api/restaurants/my-restaurant-list - 인증 정보가 없으면 401")
    void getMyRestaurants_unauthorized() throws Exception {
        mockMvc.perform(get("/api/restaurants/my-restaurant-list"))
                .andExpect(status().isUnauthorized());

        verify(restaurantService, never()).getMyRestaurants(anyInt());
    }

    @Test
    @DisplayName("GET /api/restaurants/merchant-pins - 승인된 가게 핀 목록 (비로그인 허용)")
    void getMerchantPins() throws Exception {
        RestaurantPinDto pin = new RestaurantPinDto(
                10, "맛있는집", "서울시 강남구", "02-123-4567", 37.5, 127.0, "kakao-1", 4.4, 9, 1);
        given(restaurantService.getApprovedMerchantPins()).willReturn(List.of(pin));

        mockMvc.perform(get("/api/restaurants/merchant-pins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restIdx").value(10))
                .andExpect(jsonPath("$[0].latitude").value(37.5))
                .andExpect(jsonPath("$[0].ownerUserIdx").value(1));
    }

    @Test
    @DisplayName("GET /api/restaurants/restIdx/{restIdx} - 핀 단건 조회")
    void getRestaurantByRestIdx() throws Exception {
        RestaurantPinDto pin = new RestaurantPinDto(
                10, "맛있는집", "서울시 강남구", "02-123-4567", 37.5, 127.0, "kakao-1", 0.0, 0, null);
        given(restaurantService.getRestaurantPinByRestIdx(10)).willReturn(pin);

        mockMvc.perform(get("/api/restaurants/restIdx/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kakaoId").value("kakao-1"))
                .andExpect(jsonPath("$.ownerUserIdx").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/restaurants/{restIdx}/menus - 메뉴 목록 조회")
    void getMenusByRestIdx() throws Exception {
        MenuResponseDto menu = new MenuResponseDto(
                1, "김치찌개", 9000, "http://localhost:8080/uploads/pict.jpg");
        given(restaurantService.getMenusByRestIdx(10)).willReturn(List.of(menu));

        mockMvc.perform(get("/api/restaurants/10/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menuName").value("김치찌개"))
                .andExpect(jsonPath("$[0].imageUrl").value("http://localhost:8080/uploads/pict.jpg"));
    }

    @Test
    @DisplayName("POST /api/restaurants/bulkDetails - 카카오ID 목록 일괄 상세 조회")
    void getBulkDetailsByKakaoIds() throws Exception {
        RestaurantResponseDto dto = new RestaurantResponseDto(10, "맛있는집", "서울시 강남구", 4.6, 4, 1);
        given(restaurantService.getBulkDetailsByKakaoIds(anyList())).willReturn(Map.of("kakao-1", dto));

        mockMvc.perform(post("/api/restaurants/bulkDetails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("kakao-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['kakao-1'].restIdx").value(10))
                .andExpect(jsonPath("$.['kakao-1'].reviewCount").value(4));
    }

    @Test
    @DisplayName("POST /api/restaurants/bulkDetails - 서비스 예외 시 500")
    void getBulkDetailsByKakaoIds_error() throws Exception {
        given(restaurantService.getBulkDetailsByKakaoIds(anyList()))
                .willThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/restaurants/bulkDetails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of("kakao-1"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/restaurants/kakaoId/{kakaoId} - 카카오ID 상세 조회")
    void getRestaurantDetailByKakaoId() throws Exception {
        RestaurantResponseDto dto = new RestaurantResponseDto(10, "맛있는집", "서울시 강남구", 3.0, 2, 1);
        given(restaurantService.getRestaurantDetailsByKakaoId("kakao-1")).willReturn(dto);

        mockMvc.perform(get("/api/restaurants/kakaoId/kakao-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restName").value("맛있는집"))
                .andExpect(jsonPath("$.ownerUserIdx").value(1));
    }

    // ===================== 수정 폼 / 수정 / 삭제 =====================

    @Test
    @DisplayName("GET /api/restaurants/edit/{restIdx} - 수정 폼 조회 성공")
    void getRestaurantForEdit_success() throws Exception {
        given(restaurantService.getRestaurantForEdit(10, 1)).willReturn(requestDto());

        mockMvc.perform(get("/api/restaurants/edit/10").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restName").value("맛있는집"))
                .andExpect(jsonPath("$.restBusiHours").value("10:00 - 22:00"));
    }

    @Test
    @DisplayName("GET /api/restaurants/edit/{restIdx} - 인증 정보가 없으면 401")
    void getRestaurantForEdit_unauthorized() throws Exception {
        mockMvc.perform(get("/api/restaurants/edit/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/restaurants/edit/{restIdx} - 본인 가게가 아니면 403")
    void getRestaurantForEdit_forbidden() throws Exception {
        given(restaurantService.getRestaurantForEdit(anyInt(), anyInt()))
                .willThrow(new SecurityException("본인이 등록한 가게만 조회할 수 있습니다."));

        mockMvc.perform(get("/api/restaurants/edit/10").with(authentication(new TestingAuthenticationToken(999, null))))
                .andExpect(status().isForbidden())
                .andExpect(content().string("본인이 등록한 가게만 조회할 수 있습니다."));
    }

    @Test
    @DisplayName("GET /api/restaurants/edit/{restIdx} - 가게가 없으면 404")
    void getRestaurantForEdit_notFound() throws Exception {
        given(restaurantService.getRestaurantForEdit(anyInt(), anyInt()))
                .willThrow(new IllegalStateException("해당 식당 정보를 찾을 수 없음: 999"));

        mockMvc.perform(get("/api/restaurants/edit/999").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/restaurants/update/{restIdx} - 수정 성공 시 200")
    void updateRestaurant_success() throws Exception {
        given(restaurantService.updateRestaurantByMerchant(
                eq(10), eq(1), any(RestaurantRequestDto.class), any())).willReturn(sampleRestaurant());

        mockMvc.perform(multipart("/api/restaurants/update/10")
                        .file(dtoPart())
                        .with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(content().string("가게 정보가 업데이트 되었습니다."));
    }

    @Test
    @DisplayName("POST /api/restaurants/update/{restIdx} - 인증 정보가 없으면 401")
    void updateRestaurant_unauthorized() throws Exception {
        mockMvc.perform(multipart("/api/restaurants/update/10").file(dtoPart()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/restaurants/update/{restIdx} - 본인 가게가 아니면 403")
    void updateRestaurant_forbidden() throws Exception {
        given(restaurantService.updateRestaurantByMerchant(
                anyInt(), anyInt(), any(RestaurantRequestDto.class), any()))
                .willThrow(new SecurityException("본인이 등록한 가게만 수정할 수 있습니다."));

        mockMvc.perform(multipart("/api/restaurants/update/10")
                        .file(dtoPart())
                        .with(authentication(new TestingAuthenticationToken(999, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/restaurants/update/{restIdx} - 가게가 없으면 404")
    void updateRestaurant_notFound() throws Exception {
        given(restaurantService.updateRestaurantByMerchant(
                anyInt(), anyInt(), any(RestaurantRequestDto.class), any()))
                .willThrow(new IllegalStateException("수정할 식당 정보를 찾을 수 없음"));

        mockMvc.perform(multipart("/api/restaurants/update/999")
                        .file(dtoPart())
                        .with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/restaurants/delete/{restIdx} - 삭제 성공 시 200")
    void deleteRestaurant_success() throws Exception {
        mockMvc.perform(post("/api/restaurants/delete/10").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(content().string("식당 정보와 메뉴 사진이 모두 삭제 됨"));

        verify(restaurantService).deleteRestaurant(10, 1);
    }

    @Test
    @DisplayName("POST /api/restaurants/delete/{restIdx} - 인증 정보가 없으면 401")
    void deleteRestaurant_unauthorized() throws Exception {
        mockMvc.perform(post("/api/restaurants/delete/10"))
                .andExpect(status().isUnauthorized());

        verify(restaurantService, never()).deleteRestaurant(anyInt(), anyInt());
    }

    @Test
    @DisplayName("POST /api/restaurants/delete/{restIdx} - 본인 가게가 아니면 403")
    void deleteRestaurant_forbidden() throws Exception {
        willThrow(new SecurityException("본인이 등록한 가게만 삭제할 수 있습니다."))
                .given(restaurantService).deleteRestaurant(anyInt(), anyInt());

        mockMvc.perform(post("/api/restaurants/delete/10").with(authentication(new TestingAuthenticationToken(999, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/restaurants/delete/{restIdx} - 가게가 없으면 404")
    void deleteRestaurant_notFound() throws Exception {
        willThrow(new IllegalStateException("삭제할 식당을 찾을 수 없습니다."))
                .given(restaurantService).deleteRestaurant(anyInt(), anyInt());

        mockMvc.perform(post("/api/restaurants/delete/999").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isNotFound());
    }
}
