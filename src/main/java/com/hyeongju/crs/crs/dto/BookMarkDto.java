package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class BookMarkDto {
    // 북마크 등록/조회에 쓰이는 DTO (요청/응답 공용)

    private int userIdx;
    private String kakaoId;
    private Integer restIdx;    // 이미 DB에 있는 식당인 경우에만 값 존재
    private String restName;
    private String restAddress;
    private String restTel;


}
