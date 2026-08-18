package com.hyeongju.crs.crs.dto;

// 회원가입 DTO(일반유저/상인/관리자)가 공통으로 가져야 할 필드의 getter 인터페이스
// AbstractRegistrationService에서 회원 유형에 상관없이 공통 로직을 처리하기 위해 사용
public interface BaseRegistrationDto {

    String getId();
    String getPw();
    String getConfirmPw();
    String getName();
    String getEmail();
    String getPhone();
    String getGender();

}
