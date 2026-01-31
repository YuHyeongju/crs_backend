package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class BookMarkDto {

    private int userIdx;
    private String kakaoId;
    private String restName;
    private String restAddress;
    private String restTel;


}
