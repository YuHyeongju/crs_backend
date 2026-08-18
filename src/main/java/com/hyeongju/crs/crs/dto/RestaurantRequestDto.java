package com.hyeongju.crs.crs.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter

public class RestaurantRequestDto {
    // 가게 등록/수정 요청 DTO. 메뉴 목록과 편의시설 정보를 중첩 객체로 함께 받음
    private String kakaoId;
    private String restName;
    private String restTel;
    private String restAddress;
    private String restBusiHours;
    private Double latitude;
    private Double longitude;

    @Valid
    private List<MenuList> menulist;

    private FacilitiesDto facilities;

    @Getter @Setter
    public static class MenuList{
        @NotBlank(message = "메뉴 이름은 필수 입력 값입니다.")
        private String menuName;

        @PositiveOrZero(message = "메뉴 가격은 0 이상이어야 합니다.")
        private int    menuPrice;
        private String menuPict;
    }

    @Getter @Setter
    public static class FacilitiesDto{
        private boolean wifi;
        private boolean restRoom;
        private boolean parkingAvailable;
        private boolean packingPossible;
        private boolean kakaoPay;
        private boolean samsungPay;
        private boolean kiosk;
    }
}
