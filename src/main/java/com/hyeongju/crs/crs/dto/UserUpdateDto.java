package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class UserUpdateDto {

    private String pw;
    private String email;
    private String phNum;
}
