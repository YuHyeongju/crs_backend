package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FindIdVerifyDto {
    @NotBlank(message = "이름은 필수 입력 값입니다.")
    private String name;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "인증번호는 필수 입력 값입니다.")
    private String code;
}
