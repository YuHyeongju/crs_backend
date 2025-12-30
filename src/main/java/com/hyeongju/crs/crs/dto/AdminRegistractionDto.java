package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

public class AdminRegistractionDto implements BaseRegistrationDto{

    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    @Size(min = 4, max = 50,message = "아이디는 4자 이상 50자 이하로 입력해야 합니다.")
    private String id;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "비밀번호는 최소 8자 이상이며, 영문 대소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다."
    )
    private String pw;

    @NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.")
    private String confirmPw;

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Size(max = 100,message = "이름은 100글자 이하로 입력해야합니다.")
    private String name;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "전호번호는 필수 입력 값입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$",message = "올바른 전화번호 형식이 아닙니다.")
    private String phone;

    @NotBlank(message = "성별은 필수 입력 값입니다.")
    private String gender;

    @NotBlank(message = "관리자 코드는 필수 입력사항입니다.")
    @Pattern(
            regexp = "^[a-zA-Z0-9]{7}$",
            message = "관리자 코드는 영문(대소문자)과 숫자로 구성된 7자리여야 합니다."
    )
    private String adminNum;
}
