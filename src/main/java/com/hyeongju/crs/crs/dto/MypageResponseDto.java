package com.hyeongju.crs.crs.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // null이면 json에 포함시키지 않음
public class MypageResponseDto {

    private String id;
    private String name;
    private String email;
    private String phNum;
    private String gender;
    private String role;
    private String businessNum;
    private String adminNum;
}
