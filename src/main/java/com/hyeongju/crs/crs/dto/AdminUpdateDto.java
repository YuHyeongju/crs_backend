package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class AdminUpdateDto extends UserUpdateDto{
    private String adminNum;
}
