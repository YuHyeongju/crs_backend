package com.hyeongju.crs.crs.service;

import com.hyeongju.crs.crs.domain.Role;
import com.hyeongju.crs.crs.domain.RoleName;
import com.hyeongju.crs.crs.domain.User;
import com.hyeongju.crs.crs.dto.MerchantRegistractionDto;
import com.hyeongju.crs.crs.dto.MerchantUpdateDto;
import com.hyeongju.crs.crs.dto.MypageResponseDto;
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
class MerchantServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MerchantService merchantService;

    private MerchantRegistractionDto registerDto;

    @BeforeEach
    void setUp() {
        registerDto = new MerchantRegistractionDto();
        registerDto.setId("merchant1");
        registerDto.setPw("Passw0rd!");
        registerDto.setConfirmPw("Passw0rd!");
        registerDto.setName("사장님");
        registerDto.setEmail("m@example.com");
        registerDto.setPhone("010-2222-3333");
        registerDto.setGender("M");
        // 서비스는 사업자번호를 DTO 형식과 동일하게 하이픈 포함 12자리로 검증한다
        registerDto.setBusinessNum("123-45-67890");
    }

    private User merchantUser() {
        Role role = new Role();
        role.setRoleName(RoleName.MERCHANT);

        User user = new User();
        user.setUserIdx(1);
        user.setId("merchant1");
        user.setPw("ENCODED");
        user.setName("사장님");
        user.setEmail("m@example.com");
        user.setPhNum("010-2222-3333");
        user.setGender("M");
        user.setBusinessNum("1234567890");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("상인 회원가입 성공")
    void registerMerchant_success() {
        Role role = new Role();
        role.setRoleName(RoleName.MERCHANT);

        given(userRepository.existsById("merchant1")).willReturn(false);
        given(roleRepository.findByRoleName(RoleName.MERCHANT)).willReturn(Optional.of(role));
        given(passwordEncoder.encode("Passw0rd!")).willReturn("ENCODED");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User saved = merchantService.registerMerchant(registerDto);

        assertThat(saved.getBusinessNum()).isEqualTo("123-45-67890");
        assertThat(saved.getPw()).isEqualTo("ENCODED");
        assertThat(saved.getRole().getRoleName()).isEqualTo(RoleName.MERCHANT);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("사업자 등록번호가 12자리가 아니면 IllegalArgumentException")
    void registerMerchant_invalidBusinessNumberLength() {
        // 하이픈 없는 10자리는 형식(하이픈 포함 12자리)에 맞지 않아 서비스 검증을 통과하지 못한다
        registerDto.setBusinessNum("1234567890");

        assertThatThrownBy(() -> merchantService.registerMerchant(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 사업자 등록번호 입니다.");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("사업자 등록번호가 null 이면 IllegalArgumentException")
    void registerMerchant_nullBusinessNumber() {
        registerDto.setBusinessNum(null);

        assertThatThrownBy(() -> merchantService.registerMerchant(registerDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("중복 아이디면 회원가입 실패")
    void registerMerchant_duplicateId() {
        given(userRepository.existsById("merchant1")).willReturn(true);

        assertThatThrownBy(() -> merchantService.registerMerchant(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("비밀번호 확인이 일치하지 않으면 회원가입 실패")
    void registerMerchant_passwordMismatch() {
        registerDto.setConfirmPw("Different1!");
        given(userRepository.existsById("merchant1")).willReturn(false);

        assertThatThrownBy(() -> merchantService.registerMerchant(registerDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("상인 마이페이지 조회 성공")
    void getMerchantProfile_success() {
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(merchantUser()));

        MypageResponseDto dto = merchantService.getMerchantProfile(1);

        assertThat(dto.getId()).isEqualTo("merchant1");
        assertThat(dto.getRole()).isEqualTo("MERCHANT");
        assertThat(dto.getBusinessNum()).isEqualTo("1234567890");
        assertThat(dto.getAdminNum()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 상인 마이페이지 조회 시 RuntimeException")
    void getMerchantProfile_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.getMerchantProfile(99))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("상인 정보 수정 - 비밀번호를 입력하면 재암호화된다")
    void updateMerchantProfile_withPassword() {
        User user = merchantUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("NewPassw0rd!")).willReturn("NEW_ENCODED");

        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setPw("NewPassw0rd!");
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-0000");
        dto.setBusinessNum("9876543210");

        merchantService.updateMerchantProfile(1, dto);

        assertThat(user.getPw()).isEqualTo("NEW_ENCODED");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPhNum()).isEqualTo("010-9999-0000");
        assertThat(user.getBusinessNum()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("상인 정보 수정 - 비밀번호가 비어있으면 기존 비밀번호 유지")
    void updateMerchantProfile_blankPassword() {
        User user = merchantUser();
        given(userRepository.findByUserIdx(1)).willReturn(Optional.of(user));

        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setPw("");
        dto.setEmail("new@example.com");
        dto.setPhNum("010-9999-0000");
        dto.setBusinessNum("9876543210");

        merchantService.updateMerchantProfile(1, dto);

        assertThat(user.getPw()).isEqualTo("ENCODED");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 상인 수정 시 IllegalStateException")
    void updateMerchantProfile_notFound() {
        given(userRepository.findByUserIdx(99)).willReturn(Optional.empty());

        MerchantUpdateDto dto = new MerchantUpdateDto();
        dto.setEmail("a@b.com");
        dto.setPhNum("010-0000-0000");
        dto.setBusinessNum("1234567890");

        assertThatThrownBy(() -> merchantService.updateMerchantProfile(99, dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("존재하지 않는 사용자 입니다.");
    }
}
