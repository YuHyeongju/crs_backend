package com.hyeongju.crs.crs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeongju.crs.crs.config.SecurityConfig;
import com.hyeongju.crs.crs.config.WebConfig;
import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.dto.AdminUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.dto.ReviewReportResponseDto;
import com.hyeongju.crs.crs.dto.SanctionRequestDto;
import com.hyeongju.crs.crs.dto.UserDetailsResponseDto;
import com.hyeongju.crs.crs.dto.UserListResponseDto;
import com.hyeongju.crs.crs.security.JwtAuthenticationFilter;
import com.hyeongju.crs.crs.service.AdminService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
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

@WebMvcTest(controllers = AdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, WebConfig.class, JwtAuthenticationFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;
    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    // ===================== 마이페이지 =====================

    @Test
    @DisplayName("GET /api/admins/mypage - 성공 시 200")
    void getAdminProfile_success() throws Exception {
        MypageResponseDto dto = new MypageResponseDto(
                "adminuser", "관리자", "admin@example.com", "010-1111-2222", "F", "ADMIN", null, "ABC1234");
        given(adminService.getAdminProfile(1)).willReturn(dto);

        mockMvc.perform(get("/api/admins/mypage").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("adminuser"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.adminNum").value("ABC1234"));
    }

    @Test
    @DisplayName("GET /api/admins/mypage - 인증 정보가 없으면 401")
    void getAdminProfile_unauthorized() throws Exception {
        mockMvc.perform(get("/api/admins/mypage"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인이 만료되었습니다."));
    }

    @Test
    @DisplayName("GET /api/admins/mypage - 사용자를 찾을 수 없으면 404")
    void getAdminProfile_notFound() throws Exception {
        given(adminService.getAdminProfile(1)).willThrow(new RuntimeException("해당 사용자를 찾을 수 없습니다."));

        mockMvc.perform(get("/api/admins/mypage").with(authentication(new TestingAuthenticationToken(1, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/admins/mypage/updateAdmin - 성공 시 200")
    void updateAdminProfile_success() throws Exception {
        AdminUpdateDto dto = new AdminUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-8888");
        dto.setAdminNum("ZZZ9999");

        mockMvc.perform(post("/api/admins/mypage/updateAdmin")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원 정보가 수정되었습니다."));
    }

    @Test
    @DisplayName("POST /api/admins/mypage/updateAdmin - 인증 정보가 없으면 401")
    void updateAdminProfile_unauthorized() throws Exception {
        AdminUpdateDto dto = new AdminUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-8888");
        dto.setAdminNum("ZZZ9999");

        mockMvc.perform(post("/api/admins/mypage/updateAdmin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/admins/mypage/updateAdmin - 관리자 코드가 비어있으면 400")
    void updateAdminProfile_validationError() throws Exception {
        AdminUpdateDto dto = new AdminUpdateDto();
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-8888");
        dto.setAdminNum("");

        mockMvc.perform(post("/api/admins/mypage/updateAdmin")
                        .with(authentication(new TestingAuthenticationToken(1, null)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ===================== 가게 승인/거절 =====================

    @Test
    @DisplayName("GET /api/admins/pending - 승인 대기 가게 목록")
    void getPendingList() throws Exception {
        Restaurant r = new Restaurant();
        r.setRestIdx(10);
        r.setRestName("맛있는집");
        r.setRestAddress("서울시 강남구");
        given(adminService.getPendingRestaurant()).willReturn(List.of(r));

        mockMvc.perform(get("/api/admins/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restIdx").value(10))
                .andExpect(jsonPath("$[0].restName").value("맛있는집"));
    }

    @Test
    @DisplayName("POST /api/admins/approve/{restIdx} - 승인 성공")
    void approveRestaurant() throws Exception {
        mockMvc.perform(post("/api/admins/approve/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("가게가 성공적으로 승인되었습니다."));

        verify(adminService).approvalRestaurant(10);
    }

    @Test
    @DisplayName("POST /api/admins/reject/{restIdx} - 거절 성공")
    void rejectRestaurant() throws Exception {
        mockMvc.perform(post("/api/admins/reject/10"))
                .andExpect(status().isOk())
                .andExpect(content().string("가게 등록이 거절되었습니다."));

        verify(adminService).rejectRestaurant(10);
    }

    // ===================== 유저 관리 =====================

    @Test
    @DisplayName("GET /api/admins/users - 전체 유저 목록")
    void getAllUsers() throws Exception {
        UserListResponseDto dto = new UserListResponseDto(
                1, "testuser", "USER", "test@example.com", LocalDateTime.now(), "ACTIVE", 3L, 5L);
        given(adminService.getAllUsers()).willReturn(List.of(dto));

        mockMvc.perform(get("/api/admins/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userIdx").value(1))
                .andExpect(jsonPath("$[0].userType").value("USER"))
                .andExpect(jsonPath("$[0].congestionCount").value(3))
                .andExpect(jsonPath("$[0].reviewCount").value(5));
    }

    @Test
    @DisplayName("GET /api/admins/users/{userIdx} - 유저 상세 조회")
    void getUserDetails() throws Exception {
        UserDetailsResponseDto dto = new UserDetailsResponseDto(
                1, "testuser", "USER", "test@example.com", "홍길동", "010-1234-5678",
                "M", null, null, LocalDateTime.now(), "ACTIVE", 2L, 7L);
        given(adminService.getUserDetails(1)).willReturn(dto);

        mockMvc.perform(get("/api/admins/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("testuser"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/admins/users/{userIdx}/sanction - 제재 성공")
    void sanctionUser_success() throws Exception {
        SanctionRequestDto dto = new SanctionRequestDto("부적절한 리뷰");

        mockMvc.perform(post("/api/admins/users/1/sanction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("사용자가 성공적으로 제재되었습니다."));

        verify(adminService).sanctionUser(1, "부적절한 리뷰");
    }

    @Test
    @DisplayName("POST /api/admins/users/{userIdx}/sanction - 유저가 없으면 404")
    void sanctionUser_notFound() throws Exception {
        willThrow(new RuntimeException("해당 사용자를 찾을 수 없습니다."))
                .given(adminService).sanctionUser(anyInt(), anyString());

        mockMvc.perform(post("/api/admins/users/99/sanction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SanctionRequestDto("사유"))))
                .andExpect(status().isNotFound())
                .andExpect(content().string("해당 사용자를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("POST /api/admins/users/{userIdx}/deactivate - 탈퇴 처리 성공")
    void deactivateUser_success() throws Exception {
        mockMvc.perform(post("/api/admins/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(content().string("사용자가 성공적으로 탈퇴 처리되었습니다."));

        verify(adminService).deactivateUser(1);
    }

    @Test
    @DisplayName("POST /api/admins/users/{userIdx}/deactivate - 유저가 없으면 404")
    void deactivateUser_notFound() throws Exception {
        willThrow(new RuntimeException("해당 사용자를 찾을 수 없습니다."))
                .given(adminService).deactivateUser(anyInt());

        mockMvc.perform(post("/api/admins/users/99/deactivate"))
                .andExpect(status().isNotFound());
    }

    // ===================== 리뷰 신고 =====================

    @Test
    @DisplayName("GET /api/admins/reports - 전체 신고 목록")
    void getAllReviewReports() throws Exception {
        ReviewReportResponseDto dto = new ReviewReportResponseDto(
                500, LocalDateTime.now(), "욕설", "PENDING", 1, "신고자",
                100, "리뷰 내용", 2, "작성자");
        given(adminService.getAllReviewReports()).willReturn(List.of(dto));

        mockMvc.perform(get("/api/admins/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reportIdx").value(500))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].reportedReviewAuthorName").value("작성자"));
    }

    @Test
    @DisplayName("GET /api/admins/reports/{reportIdx} - 신고 상세 조회")
    void getReviewReportDetails_success() throws Exception {
        ReviewReportResponseDto dto = new ReviewReportResponseDto(
                500, LocalDateTime.now(), "욕설", "PENDING", 1, "신고자",
                100, "리뷰 내용", 2, "작성자");
        given(adminService.getReviewReportDetails(500)).willReturn(dto);

        mockMvc.perform(get("/api/admins/reports/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("욕설"));
    }

    @Test
    @DisplayName("GET /api/admins/reports/{reportIdx} - 신고가 없으면 404")
    void getReviewReportDetails_notFound() throws Exception {
        given(adminService.getReviewReportDetails(999))
                .willThrow(new RuntimeException("Review report not found."));

        mockMvc.perform(get("/api/admins/reports/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/admins/reports/{reportIdx}/process?approve=true - 처리 성공")
    void processReviewReport_success() throws Exception {
        mockMvc.perform(post("/api/admins/reports/500/process").param("approve", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string("Review report processed successfully."));

        verify(adminService).processReviewReport(500, true);
    }

    @Test
    @DisplayName("POST /api/admins/reports/{reportIdx}/process - 신고가 없으면 404")
    void processReviewReport_notFound() throws Exception {
        willThrow(new RuntimeException("Review report not found."))
                .given(adminService).processReviewReport(anyInt(), anyBoolean());

        mockMvc.perform(post("/api/admins/reports/999/process").param("approve", "false"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/admins/reports/{reportIdx}/process - approve 파라미터가 없으면 서비스가 호출되지 않는다")
    void processReviewReport_missingParam() throws Exception {
        // 필수 쿼리 파라미터 누락은 컨트롤러 본문에 진입하기 전에 실패한다
        mockMvc.perform(post("/api/admins/reports/500/process"))
                .andExpect(status().is(org.hamcrest.Matchers.not(200)));
        verify(adminService, never()).processReviewReport(anyInt(), anyBoolean());
    }
}
