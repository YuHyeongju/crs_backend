package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordSendCodeDto {
    private String id;
    private String email;
}
