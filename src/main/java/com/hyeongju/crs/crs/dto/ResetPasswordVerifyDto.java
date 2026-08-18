package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordVerifyDto {
    // 비밀번호 재설정 2단계 요청 DTO: 인증코드 검증 후 새 비밀번호(정책 검증 포함)로 교체
    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String id;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "인증번호는 필수 입력 값입니다.")
    private String code;

    @NotBlank(message = "새 비밀번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "비밀번호는 최소 8자 이상이며, 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String newPassword;
}
