package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CongestionUpdateDto {
    // 혼잡도 제보 요청 DTO. 가게가 아직 DB에 없을 수 있어 restName/restAddress/restPhone으로 신규 등록도 지원
    private int userIdx; // 컨트롤러에서 JWT 인증 정보로 덮어씀(클라이언트 값 신뢰하지 않음)
    private Integer restIdx;
    private String kakaoId;

    @NotBlank(message = "혼잡도 상태는 필수 입력 값입니다.")
    private String congStatus;
    private String restName;
    private String restAddress;
    private String restPhone;
}
