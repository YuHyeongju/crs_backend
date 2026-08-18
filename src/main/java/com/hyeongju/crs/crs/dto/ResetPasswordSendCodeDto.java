package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordSendCodeDto {
    // 비밀번호 재설정 1단계 요청 DTO: 아이디+이메일로 본인 확인 후 인증코드 발송
    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String id;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;
}
