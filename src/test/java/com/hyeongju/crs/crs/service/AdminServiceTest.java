package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Restaurant;
import com.hyeongju.crs.crs.domain.Review;
import com.hyeongju.crs.crs.domain.ReviewReport;
import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.AdminUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.dto.ReviewReportResponseDto;
import com.hyeongju.crs.crs.dto.UserDetailsResponseDto;
import com.hyeongju.crs.crs.dto.UserListResponseDto;
import com.hyeongju.crs.crs.repository.CongestionRepository;
import com.hyeongju.crs.crs.repository.RestaurantRepository;
import com.hyeongju.crs.crs.repository.ReviewReportRepository;
import com.hyeongju.crs.crs.repository.ReviewRepository;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private CongestionRepository congestionRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ReviewReportRepository reviewReportRepository;

    @InjectMocks
    private AdminService adminService;

    private AdminRegistractionDto registerDto;

    @BeforeEach
    void setUp() {
        registerDto = new AdminRegistractionDto();
        registerDto.setId("adminuser");
        registerDto.setPw("Passw0rd!");
        registerDto.setConfirmPw("Passw0rd!");
        registerDto.setName("관리자");
        registerDto.setEmail("admin@example.com");
        registerDto.setPhone("010-1111-2222");
        registerDto.setGender("F");
        registerDto.setAdminNum("ABC1234"); // 7자리
    }

    private User adminUser() {
        Role role = new Role();
        role.setRoleName(RoleName.ADMIN);

        User user = new User();
        user.setUserIdx(1);
        user.setId("adminuser");
        user.setPw("ENCODED");
        user.setName("관리자");
        user.setEmail("admin@example.com");
        user.setPhNum("010-1111-2222");
        user.setGender("F");
        user.setAdminNum("ABC1234");
        user.setCreateTime(LocalDateTime.now());
        user.setRole(role);
        return user;
    }

    // ===================== 회원가입 =====================

    @Test
    @DisplayName("관리자 회원가입 성공")
    void registerAdmin_success() {
        Role role = new Role();
        role.setRoleName(RoleName.ADMIN);

        given(userRepository.existsById("adminuser")).willReturn(false);
        given(roleRepository.findByRoleName(RoleName.ADMIN)).willReturn(Optional.of(role));
        given(passwordEncoder.encode("Passw0rd!")).willReturn("ENCODED");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User saved = adminService.registerAdmin(registerDto);

        assertThat(saved.getAdminNum()).isEqualTo("ABC1234");
        assertThat(saved.getPw()).isEqualTo("ENCODED");
        assertThat(saved.getRole().getRoleName()).isEqualTo(RoleName.ADMIN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("관리자 코드가 7자리가 아니면 IllegalArgumentException")
    void registerAdmin_invalidAdminNum() {
        registerDto.setAdminNum("SHORT");

        assertThatThrownBy(() -> adminService.registerAdmin(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 관리자 코드 입니다.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("관리자 코드가 null 이면 IllegalArgumentException")
    void registerAdmin_nullAdminNum() {
        registerDto.setAdminNum(null);

        assertThatThrownBy(() -> adminService.registerAdmin(registerDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("중복 아이디면 회원가입 실패")
    void registerAdmin_duplicateId() {
        given(userRepository.existsById("adminuser")).willReturn(true);

        assertThatThrownBy(() -> adminService.registerAdmin(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    // ===================== 마이페이지 =====================

    @Test
    @DisplayName("관리자 마이페이지 조회 성공")
    void getAdminProfile_success() {
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(adminUser()));

        MypageResponseDto dto = adminService.getAdminProfile(1);

        assertThat(dto.getId()).isEqualTo("adminuser");
        assertThat(dto.getRole()).isEqualTo("ADMIN");
        assertThat(dto.getAdminNum()).isEqualTo("ABC1234");
        assertThat(dto.getBusinessNum()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 관리자 마이페이지 조회 시 RuntimeException")
    void getAdminProfile_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getAdminProfile(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("관리자 정보 수정 - 비밀번호를 입력하면 재암호화된다")
    void updateAdminProfile_withPassword() {
        User user = adminUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassw0rd!")).willReturn("NEW_ENCODED");

        AdminUpdateDto dto = new AdminUpdateDto();
        dto.setPw("NewPassw0rd!");
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-8888");
        dto.setAdminNum("ZZZ9999");

        adminService.updateAdminProfile(1, dto);

        assertThat(user.getPw()).isEqualTo("NEW_ENCODED");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPhNum()).isEqualTo("010-9999-8888");
        assertThat(user.getAdminNum()).isEqualTo("ZZZ9999");
    }

    @Test
    @DisplayName("관리자 정보 수정 - 비밀번호가 비어있으면 기존 비밀번호 유지")
    void updateAdminProfile_blankPasswordKeepsOld() {
        User user = adminUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));

        AdminUpdateDto dto = new AdminUpdateDto();
        dto.setPw("   ");
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-8888");
        dto.setAdminNum("ZZZ9999");

        adminService.updateAdminProfile(1, dto);

        assertThat(user.getPw()).isEqualTo("ENCODED");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 관리자 수정 시 IllegalStateException")
    void updateAdminProfile_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        AdminUpdateDto dto = new AdminUpdateDto();
        dto.setEmail("a@b.com");
        dto.setPhNum("010-0000-0000");
        dto.setAdminNum("ABC1234");

        assertThatThrownBy(() -> adminService.updateAdminProfile(99, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("존재하지 않는 사용자 입니다.");
    }

    // ===================== 가게 승인/거절 =====================

    @Test
    @DisplayName("승인 대기 가게 목록 조회")
    void getPendingRestaurant() {
        Restaurant r = new Restaurant();
        r.setRestIdx(10);
        given(restaurantRepository.findByApprovalStatusAndStatus("PENDING", "ACTIVE"))
                .willReturn(List.of(r));

        List<Restaurant> result = adminService.getPendingRestaurant();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRestIdx()).isEqualTo(10);
    }

    @Test
    @DisplayName("가게 승인 처리 성공")
    void approvalRestaurant_success() {
        Restaurant r = new Restaurant();
        r.setRestIdx(10);
        r.setApprovalStatus("PENDING");
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(r));

        adminService.approvalRestaurant(10);

        assertThat(r.getApprovalStatus()).isEqualTo("APPROVED");
        assertThat(r.getStatus()).isEqualTo("ACTIVE");
        verify(restaurantRepository).save(r);
    }

    @Test
    @DisplayName("존재하지 않는 가게 승인 시 RuntimeException")
    void approvalRestaurant_notFound() {
        given(restaurantRepository.findByRestIdx(anyInt())).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.approvalRestaurant(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("가게를 찾을 수 없음");
    }

    @Test
    @DisplayName("가게 거절 처리 성공")
    void rejectRestaurant_success() {
        Restaurant r = new Restaurant();
        r.setRestIdx(10);
        given(restaurantRepository.findByRestIdx(10)).willReturn(Optional.of(r));

        adminService.rejectRestaurant(10);

        assertThat(r.getApprovalStatus()).isEqualTo("REJECT");
        assertThat(r.getStatus()).isEqualTo("ACTIVE");
        verify(restaurantRepository).save(r);
    }

    @Test
    @DisplayName("존재하지 않는 가게 거절 시 RuntimeException")
    void rejectRestaurant_notFound() {
        given(restaurantRepository.findByRestIdx(anyInt())).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.rejectRestaurant(999))
                .isInstanceOf(RuntimeException.class);
    }

    // ===================== 유저 관리 =====================

    @Test
    @DisplayName("전체 유저 목록 조회 - 제보/리뷰 건수가 함께 담긴다")
    void getAllUsers() {
        User user = adminUser();
        given(userRepository.findAllWithRole()).willReturn(List.of(user));
        given(congestionRepository.countByUserUserIdx(1)).willReturn(3L);
        given(reviewRepository.countByUserUserIdx(1)).willReturn(5L);

        List<UserListResponseDto> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserIdx()).isEqualTo(1);
        assertThat(result.get(0).getUserType()).isEqualTo("ADMIN");
        assertThat(result.get(0).getCongestionCount()).isEqualTo(3L);
        assertThat(result.get(0).getReviewCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("유저 상세 조회 성공")
    void getUserDetails_success() {
        given(userRepository.findByUserIdxWithRole(1)).willReturn(Optional.of(adminUser()));
        given(congestionRepository.countByUserUserIdx(1)).willReturn(2L);
        given(reviewRepository.countByUserUserIdx(1)).willReturn(7L);

        UserDetailsResponseDto dto = adminService.getUserDetails(1);

        assertThat(dto.getId()).isEqualTo("adminuser");
        assertThat(dto.getUserType()).isEqualTo("ADMIN");
        assertThat(dto.getCongestionCount()).isEqualTo(2L);
        assertThat(dto.getReviewCount()).isEqualTo(7L);
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("존재하지 않는 유저 상세 조회 시 RuntimeException")
    void getUserDetails_notFound() {
        given(userRepository.findByUserIdxWithRole(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getUserDetails(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("유저 제재 시 상태가 SUSPENDED 로 변경된다")
    void sanctionUser_success() {
        User user = adminUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));

        adminService.sanctionUser(1, "부적절한 리뷰 작성");

        assertThat(user.getStatus()).isEqualTo("SUSPENDED");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 유저 제재 시 RuntimeException")
    void sanctionUser_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.sanctionUser(99, "사유"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("유저 비활성화 시 상태가 DEACTIVATED 로 변경된다")
    void deactivateUser_success() {
        User user = adminUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));

        adminService.deactivateUser(1);

        assertThat(user.getStatus()).isEqualTo("DEACTIVATED");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 유저 비활성화 시 RuntimeException")
    void deactivateUser_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deactivateUser(99))
                .isInstanceOf(RuntimeException.class);
    }

    // ===================== 리뷰 신고 =====================

    private ReviewReport sampleReport() {
        User author = adminUser();
        author.setUserIdx(2);
        author.setName("작성자");

        Review review = new Review();
        review.setReviewIdx(100);
        review.setContent("리뷰 내용");
        review.setRating(2);
        review.setUser(author);
        review.setStatus("ACTIVE");

        ReviewReport report = new ReviewReport();
        report.setReportIdx(500);
        report.setReason("욕설");
        report.setReportAt(LocalDateTime.now());
        report.setStatus("PENDING");
        report.setReporter(adminUser());
        report.setReportedReview(review);
        return report;
    }

    @Test
    @DisplayName("전체 리뷰 신고 목록 조회")
    void getAllReviewReports() {
        given(reviewReportRepository.findAll()).willReturn(List.of(sampleReport()));

        List<ReviewReportResponseDto> result = adminService.getAllReviewReports();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReportIdx()).isEqualTo(500);
        assertThat(result.get(0).getReportedReviewIdx()).isEqualTo(100);
        assertThat(result.get(0).getReportedReviewAuthorName()).isEqualTo("작성자");
    }

    @Test
    @DisplayName("리뷰 신고 상세 조회 성공")
    void getReviewReportDetails_success() {
        given(reviewReportRepository.findById(500)).willReturn(Optional.of(sampleReport()));

        ReviewReportResponseDto dto = adminService.getReviewReportDetails(500);

        assertThat(dto.getReason()).isEqualTo("욕설");
        assertThat(dto.getStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("존재하지 않는 신고 상세 조회 시 RuntimeException")
    void getReviewReportDetails_notFound() {
        given(reviewReportRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getReviewReportDetails(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Review report not found.");
    }

    @Test
    @DisplayName("리뷰 신고 승인 시 리뷰는 BLOCKED, 신고는 APPROVED 가 된다")
    void processReviewReport_approve() {
        ReviewReport report = sampleReport();
        given(reviewReportRepository.findById(500)).willReturn(Optional.of(report));

        adminService.processReviewReport(500, true);

        assertThat(report.getReportedReview().getStatus()).isEqualTo("BLOCKED");
        assertThat(report.getStatus()).isEqualTo("APPROVED");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("BLOCKED");
        verify(reviewReportRepository).save(report);
    }

    @Test
    @DisplayName("리뷰 신고 반려 시 신고만 REJECTED 가 되고 리뷰는 유지된다")
    void processReviewReport_reject() {
        ReviewReport report = sampleReport();
        given(reviewReportRepository.findById(500)).willReturn(Optional.of(report));

        adminService.processReviewReport(500, false);

        assertThat(report.getStatus()).isEqualTo("REJECTED");
        assertThat(report.getReportedReview().getStatus()).isEqualTo("ACTIVE");
        verify(reviewRepository, never()).save(any(Review.class));
        verify(reviewReportRepository).save(report);
    }

    @Test
    @DisplayName("존재하지 않는 신고 처리 시 RuntimeException")
    void processReviewReport_notFound() {
        given(reviewReportRepository.findById(999)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.processReviewReport(999, true))
                .isInstanceOf(RuntimeException.class);
    }
}
