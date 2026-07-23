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
    private int userIdx;
    private Integer restIdx;
    private String kakaoId;

    @NotBlank(message = "혼잡도 상태는 필수 입력 값입니다.")
    private String congStatus;
    private String restName;
    private String restAddress;
    private String restPhone;
}
