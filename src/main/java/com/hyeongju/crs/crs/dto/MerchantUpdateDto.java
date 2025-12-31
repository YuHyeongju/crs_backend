package com.hyeongju.crs.crs.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class MerchantUpdateDto extends UserUpdateDto{

    private String businessNum;

}
