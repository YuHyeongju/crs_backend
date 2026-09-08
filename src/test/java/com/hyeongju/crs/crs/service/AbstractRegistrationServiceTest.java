package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.UserRegistractionDto;
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

/**
 * AbstractRegistrationService 는 추상 클래스이므로 테스트 전용 구체 클래스를 통해 검증한다.
 * registerCommonFields 는 protected 이지만 테스트가 같은 패키지에 있으므로 직접 호출 가능하다.
 */
@ExtendWith(MockitoExtension.class)
class AbstractRegistrationServiceTest {

    /** 테스트용 구체 구현체 (추가 로직 없음) */
    static class TestRegistrationService extends AbstractRegistrationService {
        TestRegistrationService(UserRepository userRepository,
                                RoleRepository roleRepository,
                                PasswordEncoder passwordEncoder) {
            super(userRepository, roleRepository, passwordEncoder);
        }
    }

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TestRegistrationService service;

    private UserRegistractionDto dto;

    @BeforeEach
    void setUp() {
        dto = new UserRegistractionDto();
        dto.setId("testuser");
        dto.setPw("Passw0rd!");
        dto.setConfirmPw("Passw0rd!");
        dto.setName("홍길동");
        dto.setEmail("test@example.com");
        dto.setPhone("010-1234-5678");
        dto.setGender("M");
    }

    @Test
    @DisplayName("공통 회원가입 필드 생성 성공 - 비밀번호는 암호화되고 역할/가입시각이 채워진다")
    void registerCommonFields_success() {
        Role role = new Role();
        role.setRoleName(RoleName.USER);

        given(userRepository.existsById("testuser")).willReturn(false);
        given(roleRepository.findByRoleName(RoleName.USER)).willReturn(Optional.of(role));
        given(passwordEncoder.encode("Passw0rd!")).willReturn("ENCODED");

        User result = service.registerCommonFields(dto, RoleName.USER);

        assertThat(result.getId()).isEqualTo("testuser");
        assertThat(result.getPw()).isEqualTo("ENCODED");
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getPhNum()).isEqualTo("010-1234-5678");
        assertThat(result.getGender()).isEqualTo("M");
        assertThat(result.getRole()).isSameAs(role);
        assertThat(result.getCreateTime()).isNotNull();
        // 저장은 하위 서비스가 담당하므로 여기서는 save 호출이 없어야 한다
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이미 존재하는 아이디면 IllegalArgumentException")
    void registerCommonFields_duplicateId() {
        given(userRepository.existsById("testuser")).willReturn(true);

        assertThatThrownBy(() -> service.registerCommonFields(dto, RoleName.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 IllegalArgumentException")
    void registerCommonFields_passwordMismatch() {
        dto.setConfirmPw("Different1!");
        given(userRepository.existsById("testuser")).willReturn(false);

        assertThatThrownBy(() -> service.registerCommonFields(dto, RoleName.USER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("Role 테이블에 해당 역할이 없으면 새로 생성해서 저장한다")
    void registerCommonFields_createsRoleWhenMissing() {
        given(userRepository.existsById("testuser")).willReturn(false);
        given(roleRepository.findByRoleName(RoleName.MERCHANT)).willReturn(Optional.empty());
        given(roleRepository.save(any(Role.class))).willAnswer(inv -> inv.getArgument(0));
        given(passwordEncoder.encode(anyString())).willReturn("ENCODED");

        User result = service.registerCommonFields(dto, RoleName.MERCHANT);

        assertThat(result.getRole().getRoleName()).isEqualTo(RoleName.MERCHANT);
        verify(roleRepository).save(any(Role.class));
    }
}
