package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ResetPasswordVerifyDto {
    private String id;
    private String email;
    private String code;
    private String newPassword;
}
