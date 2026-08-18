package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class AdminUpdateDto extends UserUpdateDto{
    // 관리자 마이페이지 수정 요청 DTO. 공통 필드(pw/email/phNum)는 UserUpdateDto에서 상속받음

    @NotBlank(message = "관리자 인증 코드는 필수 입력 값입니다.")
    private String adminNum;
}
