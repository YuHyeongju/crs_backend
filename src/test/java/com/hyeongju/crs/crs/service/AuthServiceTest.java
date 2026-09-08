package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.RefreshToken;
import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.AdminRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.repository.RefreshTokenRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import com.hyeongju.crs.crs.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private UserService userService;
    @Mock
    private MerchantService merchantService;
    @Mock
    private AdminService adminService;

    @InjectMocks
    private AuthService authService;

    private User activeUser() {
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        User user = new User();
        user.setUserIdx(1);
        user.setId("testuser");
        user.setPw("ENCODED");
        user.setName("홍길동");
        user.setStatus("ACTIVE");
        user.setRole(role);
        return user;
    }

    // ===================== 중복 확인 =====================

    @Test
    @DisplayName("아이디 중복 확인")
    void existsById() {
        given(userRepository.existsById("dup")).willReturn(true);
        assertThat(authService.existsById("dup")).isTrue();
    }

    @Test
    @DisplayName("전화번호 중복 확인")
    void existsByPhone() {
        given(userRepository.existsByPhNum("010-1234-5678")).willReturn(false);
        assertThat(authService.existsByPhone("010-1234-5678")).isFalse();
    }

    @Test
    @DisplayName("사업자 번호 중복 확인")
    void existsByBusinessNum() {
        given(userRepository.existsByBusinessNum("123-45-67890")).willReturn(true);
        assertThat(authService.existsByBusinessNum("123-45-67890")).isTrue();
    }

    @Test
    @DisplayName("관리자 코드 중복 확인")
    void existsByAdminNum() {
        given(userRepository.existsByAdminNum("ABC1234")).willReturn(true);
        assertThat(authService.existsByAdminNum("ABC1234")).isTrue();
    }

    // ===================== 회원가입 =====================

    @Test
    @DisplayName("일반 유저 회원가입 - UserService로 위임된다")
    void registerUser() {
        UserRegistractionDto dto = new UserRegistractionDto();
        dto.setId("newuser");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("신규");
        dto.setEmail("new@example.com");
        dto.setPhone("010-1111-1111");
        dto.setGender("M");

        authService.registerUser(dto);

        verify(userService).registerUser(dto);
    }

    @Test
    @DisplayName("상인 회원가입 - MerchantService로 위임된다")
    void registerMerchant() {
        MerchantRegistractionDto dto = new MerchantRegistractionDto();
        dto.setId("merchant1");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("사장님");
        dto.setEmail("m@example.com");
        dto.setPhone("010-2222-2222");
        dto.setGender("M");
        dto.setBusinessNum("123-45-67890");

        authService.registerMerchant(dto);

        verify(merchantService).registerMerchant(dto);
    }

    @Test
    @DisplayName("관리자 회원가입 - AdminService로 위임된다")
    void registerAdmin_createsRoleWhenMissing() {
        AdminRegistractionDto dto = new AdminRegistractionDto();
        dto.setId("admin1");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("관리자");
        dto.setEmail("a@example.com");
        dto.setPhone("010-3333-3333");
        dto.setGender("F");
        dto.setAdminNum("ABC1234");

        authService.registerAdmin(dto);

        verify(adminService).registerAdmin(dto);
    }

    // ===================== 로그인 =====================

    @Test
    @DisplayName("로그인 성공")
    void authenticate_success() {
        User user = activeUser();
        given(userRepository.findById("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("raw", "ENCODED")).willReturn(true);

        User result = authService.authenticate("testuser", "raw");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 로그인 시 예외")
    void authenticate_userNotFound() {
        given(userRepository.findById("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("nope", "raw"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("존재하지 않는 사용자 ID 입니다.");
    }

    @Test
    @DisplayName("비밀번호 불일치 시 예외")
    void authenticate_wrongPassword() {
        given(userRepository.findById("testuser")).willReturn(Optional.of(activeUser()));
        given(passwordEncoder.matches("wrong", "ENCODED")).willReturn(false);

        assertThatThrownBy(() -> authService.authenticate("testuser", "wrong"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("탈퇴한 계정으로 로그인 시 예외")
    void authenticate_withdrawn() {
        User user = activeUser();
        user.setStatus("WITHDRAWN");
        given(userRepository.findById("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        assertThatThrownBy(() -> authService.authenticate("testuser", "raw"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("탈퇴한 계정입니다.");
    }

    @Test
    @DisplayName("정지된 계정으로 로그인 시 예외")
    void authenticate_suspended() {
        User user = activeUser();
        user.setStatus("SUSPENDED");
        given(userRepository.findById("testuser")).willReturn(Optional.of(user));
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        assertThatThrownBy(() -> authService.authenticate("testuser", "raw"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이용이 제한된 계정입니다. 관리자에게 문의하세요.");
    }

    // ===================== 리프레시 토큰 =====================

    @Test
    @DisplayName("rememberMe=true 면 30일 만료 리프레시 토큰이 발급된다")
    void issueRefreshToken_rememberMe() {
        String token = authService.issueRefreshToken(1, true);

        assertThat(token).isNotBlank();
        verify(refreshTokenRepository).deleteByUserIdx(1);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserIdx()).isEqualTo(1);
        assertThat(saved.getToken()).isEqualTo(token);
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
    }

    @Test
    @DisplayName("rememberMe=false 면 1일 만료 리프레시 토큰이 발급된다")
    void issueRefreshToken_default() {
        authService.issueRefreshToken(2, false);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getExpiresAt()).isBefore(LocalDateTime.now().plusDays(2));
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 재발급 성공")
    void refreshAccessToken_success() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("valid-token");
        rt.setUserIdx(1);
        rt.setExpiresAt(LocalDateTime.now().plusDays(1));

        given(refreshTokenRepository.findByToken("valid-token")).willReturn(Optional.of(rt));
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(activeUser()));
        given(jwtUtil.generateAccessToken(1, "USER")).willReturn("new-access-token");

        Map<String, Object> result = authService.refreshAccessToken("valid-token");

        assertThat(result.get("accessToken")).isEqualTo("new-access-token");
        assertThat(result.get("userIdx")).isEqualTo(1);
        assertThat(result.get("role")).isEqualTo("USER");
        assertThat(result.get("name")).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰이면 예외")
    void refreshAccessToken_invalidToken() {
        given(refreshTokenRepository.findByToken("bad")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken("bad"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("유효하지 않은 토큰입니다.");
    }

    @Test
    @DisplayName("만료된 리프레시 토큰이면 삭제 후 예외")
    void refreshAccessToken_expired() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("expired");
        rt.setUserIdx(1);
        rt.setExpiresAt(LocalDateTime.now().minusDays(1));
        given(refreshTokenRepository.findByToken("expired")).willReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refreshAccessToken("expired"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("만료된 리프레시 토큰입니다.");
        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    @DisplayName("토큰은 유효하지만 사용자를 찾을 수 없으면 예외")
    void refreshAccessToken_userNotFound() {
        RefreshToken rt = new RefreshToken();
        rt.setToken("valid-token");
        rt.setUserIdx(77);
        rt.setExpiresAt(LocalDateTime.now().plusDays(1));
        given(refreshTokenRepository.findByToken("valid-token")).willReturn(Optional.of(rt));
        given(userRepository.findByUserIdx(77)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshAccessToken("valid-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("로그아웃 시 리프레시 토큰이 삭제된다")
    void deleteRefreshToken_present() {
        RefreshToken rt = new RefreshToken();
        given(refreshTokenRepository.findByToken("t")).willReturn(Optional.of(rt));

        authService.deleteRefreshToken("t");

        verify(refreshTokenRepository).delete(rt);
    }

    @Test
    @DisplayName("존재하지 않는 토큰 삭제 요청은 예외 없이 무시된다")
    void deleteRefreshToken_absent() {
        given(refreshTokenRepository.findByToken("none")).willReturn(Optional.empty());

        authService.deleteRefreshToken("none");

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    // ===================== 탈퇴 =====================

    @Test
    @DisplayName("회원 탈퇴 시 상태가 WITHDRAWN 으로 변경된다")
    void withdraw_success() {
        User user = activeUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));

        authService.withdraw(1);

        assertThat(user.getStatus()).isEqualTo("WITHDRAWN");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 탈퇴 시 예외")
    void withdraw_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.withdraw(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("존재하지 않는 사용자 입니다.");
    }
}
