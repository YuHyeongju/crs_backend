package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class UserLoginDto {
    // 로그인 요청 바디 DTO

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String id;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private String pw;

    private boolean rememberMe; // true면 리프레시 토큰 유효기간을 길게(로그인 유지) 설정
}
