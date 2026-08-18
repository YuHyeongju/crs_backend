package com.hyeongju.crs.crs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SanctionRequestDto {
    // 관리자가 유저를 제재(정지/탈퇴 등)할 때 사유를 전달하는 요청 DTO
    private String reason;
}
