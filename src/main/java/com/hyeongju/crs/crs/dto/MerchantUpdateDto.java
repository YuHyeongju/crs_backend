package com.hyeongju.crs.crs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class MerchantUpdateDto extends UserUpdateDto{

    @NotBlank(message = "사업자 등록번호는 필수 입력 값입니다.")
    private String businessNum;

}
