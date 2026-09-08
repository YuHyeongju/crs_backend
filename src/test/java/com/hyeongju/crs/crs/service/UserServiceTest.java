package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
import com.hyeongju.crs.crs.dto.UserUpdateDto;
import com.hyeongju.crs.crs.repository.RoleRepository;
import com.hyeongju.crs.crs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailService emailService;
    @Mock
    private VerificationService verificationService;

    @InjectMocks
    private UserService userService;

    private UserRegistractionDto registerDto;

    @BeforeEach
    void setUp() {
        registerDto = new UserRegistractionDto();
        registerDto.setId("testuser");
        registerDto.setPw("Passw0rd!");
        registerDto.setConfirmPw("Passw0rd!");
        registerDto.setName("홍길동");
        registerDto.setEmail("test@example.com");
        registerDto.setPhone("010-1234-5678");
        registerDto.setGender("M");
    }

    private User normalUser() {
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        User user = new User();
        user.setUserIdx(1);
        user.setId("testuser");
        user.setPw("ENCODED");
        user.setName("홍길동");
        user.setEmail("test@example.com");
        user.setPhNum("010-1234-5678");
        user.setGender("M");
        user.setRole(role);
        return user;
    }

    // ===================== 회원가입 =====================

    @Test
    @DisplayName("일반 유저 회원가입 성공")
    void registerUser_success() {
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        given(userRepository.existsById("testuser")).willReturn(false);
        given(roleRepository.findByRoleName(RoleName.USER)).willReturn(Optional.of(role));
        given(passwordEncoder.encode("Passw0rd!")).willReturn("ENCODED");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User saved = userService.registerUser(registerDto);

        assertThat(saved.getId()).isEqualTo("testuser");
        assertThat(saved.getPw()).isEqualTo("ENCODED");
        assertThat(saved.getRole().getRoleName()).isEqualTo(RoleName.USER);
        assertThat(saved.getBusinessNum()).isNull();
        assertThat(saved.getAdminNum()).isNull();
    }

    @Test
    @DisplayName("중복 아이디면 회원가입 실패")
    void registerUser_duplicateId() {
        given(userRepository.existsById("testuser")).willReturn(true);

        assertThatThrownBy(() -> userService.registerUser(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("비밀번호 확인 불일치 시 회원가입 실패")
    void registerUser_passwordMismatch() {
        registerDto.setConfirmPw("Other1234!");
        given(userRepository.existsById("testuser")).willReturn(false);

        assertThatThrownBy(() -> userService.registerUser(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }

    // ===================== 마이페이지 =====================

    @Test
    @DisplayName("유저 마이페이지 조회 성공")
    void getUserProfile_success() {
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(normalUser()));

        MypageResponseDto dto = userService.getUserProfile(1);

        assertThat(dto.getId()).isEqualTo("testuser");
        assertThat(dto.getName()).isEqualTo("홍길동");
        assertThat(dto.getRole()).isEqualTo("USER");
        assertThat(dto.getBusinessNum()).isNull();
        assertThat(dto.getAdminNum()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 유저 마이페이지 조회 시 RuntimeException")
    void getUserProfile_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserProfile(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("유저 정보 수정 - 비밀번호 입력 시 재암호화")
    void updateUserProfile_withPassword() {
        User user = normalUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassw0rd!")).willReturn("NEW_ENCODED");

        UserUpdateDto dto = new UserUpdateDto();
        dto.setPw("NewPassw0rd!");
        dto.setEmail("new@example.com");
        dto.setPhNum("010-5555-6666");

        userService.updateUserProfile(1, dto);

        assertThat(user.getPw()).isEqualTo("NEW_ENCODED");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPhNum()).isEqualTo("010-5555-6666");
    }

    @Test
    @DisplayName("유저 정보 수정 - 비밀번호가 null 이면 기존 비밀번호 유지")
    void updateUserProfile_nullPassword() {
        User user = normalUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));

        UserUpdateDto dto = new UserUpdateDto();
        dto.setPw(null);
        dto.setEmail("new@example.com");
        dto.setPhNum("010-5555-6666");

        userService.updateUserProfile(1, dto);

        assertThat(user.getPw()).isEqualTo("ENCODED");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 유저 수정 시 IllegalStateException")
    void updateUserProfile_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("a@b.com");
        dto.setPhNum("010-0000-0000");

        assertThatThrownBy(() -> userService.updateUserProfile(99, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("존재하지 않는 사용자 입니다.");
    }

    // ===================== 아이디 찾기 =====================

    @Test
    @DisplayName("아이디 찾기 인증코드 발송 성공")
    void sendFindIdCode_success() {
        given(userRepository.findByNameAndEmail("홍길동", "test@example.com"))
                .willReturn(Optional.of(normalUser()));
        given(verificationService.generateAndStore("findId:test@example.com")).willReturn("123456");

        userService.sendFindIdCode("홍길동", "test@example.com");

        verify(emailService).sendVerificationCode("test@example.com", "123456");
    }

    @Test
    @DisplayName("일치하는 회원이 없으면 인증코드를 발송하지 않는다")
    void sendFindIdCode_userNotFound() {
        given(userRepository.findByNameAndEmail("없음", "none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.sendFindIdCode("없음", "none@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일치하는 회원 정보가 없습니다.");
        verify(emailService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    @DisplayName("아이디 찾기 검증 성공 시 마스킹된 아이디를 반환한다")
    void verifyFindId_success() {
        given(userRepository.findByNameAndEmail("홍길동", "test@example.com"))
                .willReturn(Optional.of(normalUser()));
        given(verificationService.verify("findId:test@example.com", "123456")).willReturn(true);

        String masked = userService.verifyFindId("홍길동", "test@example.com", "123456");

        // "testuser"(8자) -> 앞 2글자 + '*' 5개 + 마지막 1글자
        assertThat(masked).isEqualTo("te*****r");
    }

    @Test
    @DisplayName("아이디가 2글자 이하면 첫 글자 + *** 로 마스킹된다")
    void verifyFindId_shortIdMasking() {
        User user = normalUser();
        user.setId("ab");
        given(userRepository.findByNameAndEmail("홍길동", "test@example.com")).willReturn(Optional.of(user));
        given(verificationService.verify(anyString(), anyString())).willReturn(true);

        assertThat(userService.verifyFindId("홍길동", "test@example.com", "123456")).isEqualTo("a***");
    }

    @Test
    @DisplayName("인증코드가 틀리면 아이디 찾기 실패")
    void verifyFindId_invalidCode() {
        given(userRepository.findByNameAndEmail("홍길동", "test@example.com"))
                .willReturn(Optional.of(normalUser()));
        given(verificationService.verify("findId:test@example.com", "000000")).willReturn(false);

        assertThatThrownBy(() -> userService.verifyFindId("홍길동", "test@example.com", "000000"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("인증번호가 올바르지 않거나 만료되었습니다.");
    }

    @Test
    @DisplayName("아이디 찾기 검증 - 회원 정보가 없으면 예외")
    void verifyFindId_userNotFound() {
        given(userRepository.findByNameAndEmail(anyString(), anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyFindId("없음", "none@example.com", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일치하는 회원 정보가 없습니다.");
    }

    // ===================== 비밀번호 재설정 =====================

    @Test
    @DisplayName("비밀번호 재설정 인증코드 발송 성공")
    void sendResetPasswordCode_success() {
        given(userRepository.findByIdAndEmail("testuser", "test@example.com"))
                .willReturn(Optional.of(normalUser()));
        given(verificationService.generateAndStore("resetPw:test@example.com")).willReturn("654321");

        userService.sendResetPasswordCode("testuser", "test@example.com");

        verify(emailService).sendVerificationCode("test@example.com", "654321");
    }

    @Test
    @DisplayName("비밀번호 재설정 - 회원 정보가 없으면 예외")
    void sendResetPasswordCode_userNotFound() {
        given(userRepository.findByIdAndEmail(anyString(), anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.sendResetPasswordCode("nope", "none@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일치하는 회원 정보가 없습니다.");
        verify(emailService, never()).sendVerificationCode(anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 성공 시 새 비밀번호가 암호화되어 반영된다")
    void verifyAndResetPassword_success() {
        User user = normalUser();
        given(userRepository.findByIdAndEmail("testuser", "test@example.com")).willReturn(Optional.of(user));
        given(verificationService.verify("resetPw:test@example.com", "654321")).willReturn(true);
        given(passwordEncoder.encode("BrandNew1!")).willReturn("BRAND_NEW_ENCODED");

        userService.verifyAndResetPassword("testuser", "test@example.com", "654321", "BrandNew1!");

        assertThat(user.getPw()).isEqualTo("BRAND_NEW_ENCODED");
    }

    @Test
    @DisplayName("인증코드가 틀리면 비밀번호가 변경되지 않는다")
    void verifyAndResetPassword_invalidCode() {
        User user = normalUser();
        given(userRepository.findByIdAndEmail("testuser", "test@example.com")).willReturn(Optional.of(user));
        given(verificationService.verify("resetPw:test@example.com", "000000")).willReturn(false);

        assertThatThrownBy(() -> userService.verifyAndResetPassword(
                "testuser", "test@example.com", "000000", "BrandNew1!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("인증번호가 올바르지 않거나 만료되었습니다.");
        assertThat(user.getPw()).isEqualTo("ENCODED");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 - 회원 정보가 없으면 예외")
    void verifyAndResetPassword_userNotFound() {
        given(userRepository.findByIdAndEmail(anyString(), anyString())).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyAndResetPassword(
                "nope", "none@example.com", "123456", "BrandNew1!"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일치하는 회원 정보가 없습니다.");
    }
}
