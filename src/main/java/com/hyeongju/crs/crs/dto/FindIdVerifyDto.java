package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FindIdVerifyDto {
    private String name;
    private String email;
    private String code;
}
