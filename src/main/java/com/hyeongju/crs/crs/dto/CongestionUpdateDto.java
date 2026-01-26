package com.hyeongju.crs.crs.dto;

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
    private String kakaoId;
    private String congStatus;
    private String restName;
    private String restAddress;
    private String restPhone;
}
