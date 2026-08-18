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
@JsonInclude(JsonInclude.Include.NON_NULL) // businessNum/adminNum처럼 회원 유형별로 없는 필드는 응답에서 제외
public class MypageResponseDto {
    // 마이페이지 조회 응답 DTO

    private String id;
    private String name;
    private String email;
    private String phNum;
    private String gender;
    private String role;
    private String businessNum; // 상인만 값 존재
    private String adminNum;    // 관리자만 값 존재
}
